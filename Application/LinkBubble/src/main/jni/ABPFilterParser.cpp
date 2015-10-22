#include "ABPFilterParser.h"
#include <string.h>

const int maxLineLength = 2048;
const char *separatorCharacters = ":?/=^";

enum FilterParseState {
  FPStart,
  FPPastWhitespace,
  FPOneBar,
  FPOneAt,
  FPData
};

bool isSeparatorChar(char c) {
  const char *p = separatorCharacters;
  while (*p != 0) {
    if (*p == c) {
      return true;
    }
    ++p;
  };
  return false;
}

int findFirstSeparatorChar(const char *input, const char *end) {
  const char *p = input;
  while (p != end) {
    if (isSeparatorChar(*p)) {
      return p - input;
    }
    p++;
  }
  return -1;
}

void parseFilter(const char *input, Filter &f) {
  const char *end = input;
  while (*end != '\0') end++;
  parseFilter(input, end, f);
}

// Not currently multithreaded safe due to the static buffer named 'data'
void parseFilter(const char *input, const char *end, Filter &f) {
  FilterParseState parseState = FPStart;
  const char *p = input;
  char data[maxLineLength];
  memset(data, 0, sizeof data);
  int i = 0;

  while (p != end) {
    // Check for the filter being too long
    if ((p - input) >= maxLineLength - 1) {
      return;
    }

    if (parseState == FPOneBar && *p != '|') {
      parseState = FPData;
      f.filterType = static_cast<FilterType>(f.filterType | FTLeftAnchored);
    }

    switch (*p) {
      case '|':
        if (parseState == FPStart || parseState == FPPastWhitespace) {
          parseState = FPOneBar;
          p++;
          continue;
        } else if (parseState == FPOneBar) {
          parseState = FPOneBar;
          f.filterType = static_cast<FilterType>(f.filterType | FTHostAnchored);
          parseState = FPData;
          p++;

          int len = findFirstSeparatorChar(p, end);
          if (len == -1) {
            len = end - p;
          }

          f.host = new char[len + 1];
          f.host[len] = '\0';
          memcpy(f.host, p, len);

          continue;
        } else {
          f.filterType = static_cast<FilterType>(f.filterType | FTRightAnchored);
          parseState = FPData;
          p++;
          continue;
        }
        break;
      case '@':
        if (parseState == FPStart || parseState == FPPastWhitespace) {
          parseState = FPOneAt;
          p++;
          continue;
        } else if (parseState == FPOneAt) {
          parseState = FPOneBar;
          f.filterType = FTException;
          parseState = FPPastWhitespace;
          p++;
          continue;
        }
        break;
      case '!':
      case '[':
        if (parseState == FPStart || parseState == FPPastWhitespace) {
          f.filterType = FTComment;
          // Wed don't care about comments right now
          return;
        }
        break;
      case '\r':
      case '\n':
      case '\t':
      case ' ':
        // Skip leading whitespace
        if (parseState == FPStart) {
          p++;
          continue;
        }
        break;
      case '/':
        if ((parseState == FPStart || parseState == FPPastWhitespace) && input[strlen(input) -1] == '/') {
          // Just copy out the whole regex and return early
          int len = strlen(input) - i - 1;
          f.data = new char[len];
          f.data[len - 1] = '\0';
          memcpy(f.data, input + i + 1, len - 1);
          f.filterType = FTRegex;
          return;
        }
        break;

      case '$':
        f.parseOptions(p + 1);
        data[i] = '\0';
        f.data = new char[i + 1];
        memcpy(f.data, data, i + 1);
        return;

      case '#':
        if (*(p+1) == '#') {
          // TODO
          f.filterType = FTElementHiding;
          return;
        } else if (*(p+1) == '@') {
          f.filterType = FTElementHidingException;
          return;
        }

      default:
        parseState = FPData;
        break;
    }

    data[i] = *p;
    i++;
    p++;
  }

  if (parseState == FPStart) {
    f.filterType = FTEmpty;
    return;
  }

  data[i] = '\0';
  f.data = new char[i + 1];
  memcpy(f.data, data, i + 1);
}


ABPFilterParser::ABPFilterParser() : filters(nullptr),
  htmlRuleFilters(nullptr),
  exceptionFilters(nullptr),
  noFingerprintFilters(nullptr),
  numFilters(0),
  numHtmlRuleFilters(0),
  numExceptionFilters(0),
  numNoFingerprintFilters(0) {
}

ABPFilterParser::~ABPFilterParser() {
  if (filters) {
    delete[] filters;
  }
  if( htmlRuleFilters) {
    delete[] htmlRuleFilters;
  }
  if (exceptionFilters) {
    delete[] exceptionFilters;
  }
  if (noFingerprintFilters) {
   delete[] noFingerprintFilters;
  }
}

bool ABPFilterParser::hasMatchingFilters(Filter *filter, int &numFilters, const char *input, FilterOption contextOption, const char *contextDomain) {
  for (int i = 0; i < numFilters; i++) {
    if (filter->matches(input, contextOption, contextDomain)) {
      return true;
    }
    filter++;
  }
  return false;
}

bool ABPFilterParser::matches(const char *input, FilterOption contextOption, const char *contextDomain) {
  if (hasMatchingFilters(filters, numFilters, input, contextOption, contextDomain)) {
    if (hasMatchingFilters(exceptionFilters, numExceptionFilters, input, contextOption, contextDomain)) {
      return false;
    }
    return true;
  }
  return false;
}

// Parses the filter data into a few collections of filters and enables efficent querying
bool ABPFilterParser::parse(const char *input) {
  const char *p = input;
  const char *lineStart = p;

  int newNumFilters = 0;
  int newNumHtmlRuleFilters = 0;
  int newNumExceptionFilters = 0;
  int newNumNoFingerprintFilters = 0;

  // Parsing does 2 passes, one just to determine the type of information we'll need to setup.
  // Note that the library will be used on a variety of builds so sometimes we won't even have STL
  // So we can't use something like a vector here.
  while (true) {
    if (*p == '\n' || *p == '\0') {
      Filter f;
      parseFilter(lineStart, p, f);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
          newNumExceptionFilters++;
          break;
        case FTElementHiding:
          newNumHtmlRuleFilters++;
          break;
        case FTElementHidingException:
          newNumHtmlRuleFilters++;
          break;
        case FTEmpty:
        case FTComment:
          // No need to store comments
          break;
        default:
          // TODO: check if no fingerprint and if so update numNoFingerprintFilters
          newNumFilters++;
          break;
      }
      lineStart = p + 1;
    }

    if (*p == '\0') {
      break;
    }

    p++;
  };


  Filter *newFilters = new Filter[newNumFilters + numFilters];
  Filter *newHtmlRuleFilters = new Filter[newNumHtmlRuleFilters + numHtmlRuleFilters];
  Filter *newExceptionFilters = new Filter[newNumExceptionFilters + numExceptionFilters];
  Filter *newNoFingerprintFilters = new Filter[newNumNoFingerprintFilters + numNoFingerprintFilters];

  memset(newFilters, 0, sizeof(Filter) * (newNumFilters + numFilters));
  memset(newHtmlRuleFilters, 0, sizeof(Filter) * (newNumHtmlRuleFilters + numHtmlRuleFilters));
  memset(newExceptionFilters, 0, sizeof(Filter) * (newNumExceptionFilters + numExceptionFilters));
  memset(newNoFingerprintFilters, 0, sizeof(Filter) * (newNumNoFingerprintFilters + numNoFingerprintFilters));

  Filter *curFilters = newFilters;
  Filter *curHtmlRuleFilters = newHtmlRuleFilters;
  Filter *curExceptionFilters = newExceptionFilters;
  Filter *curNoFingerprintFilters = newNoFingerprintFilters;

  // If we've had a parse before copy the old data into the new data structure
  if (filters || htmlRuleFilters || exceptionFilters || noFingerprintFilters) {
    // Copy the old data in
    memcpy(newFilters, filters, sizeof(Filter) * numFilters);
    memcpy(newHtmlRuleFilters, htmlRuleFilters, sizeof(Filter) * numHtmlRuleFilters);
    memcpy(newExceptionFilters, exceptionFilters, sizeof(Filter) * numExceptionFilters);
    memcpy(newNoFingerprintFilters, noFingerprintFilters, sizeof(Filter) * (numNoFingerprintFilters));

    // Adjust the current pointers to be just after the copied in data
    curFilters += numFilters;
    curHtmlRuleFilters += numHtmlRuleFilters;
    curExceptionFilters += numExceptionFilters;
    curNoFingerprintFilters += numNoFingerprintFilters;
  }

  // And finally update with the new counts
  numFilters += newNumFilters;
  numHtmlRuleFilters += newNumHtmlRuleFilters;
  numExceptionFilters += newNumExceptionFilters;
  numNoFingerprintFilters += newNumNoFingerprintFilters;

  // Adjust the new member list pointers
  filters = newFilters;
  htmlRuleFilters = newHtmlRuleFilters;
  exceptionFilters = newExceptionFilters;
  noFingerprintFilters = newNoFingerprintFilters;

  p = input;
  lineStart = p;

  while (true) {
    if (*p == '\n' || *p == '\0') {
      Filter f;
      parseFilter(lineStart, p, f);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
          (*curExceptionFilters).swap(f);
          curExceptionFilters++;
          break;
        case FTElementHiding:
        case FTElementHidingException:
          (*curHtmlRuleFilters).swap(f);
          curHtmlRuleFilters++;
          break;
        case FTEmpty:
        case FTComment:
          // No need to store
          break;
        default:
          // TODO: check if no fingerprint here and update noFingerprintFilters instead
          (*curFilters).swap(f);
          curFilters++;
          break;
      }
      lineStart = p + 1;
    }

    if (*p == '\0') {
      break;
    }

    p++;
  };

  return true;
}

