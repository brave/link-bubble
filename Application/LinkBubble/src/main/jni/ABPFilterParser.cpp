#include "ABPFilterParser.h"
#include <string.h>
<<<<<<< HEAD
<<<<<<< HEAD
#include <stdio.h>

#ifdef PERF_STATS
#include <iostream>
using namespace std;
#endif

=======
#include <stdio.h>

>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
#define DISABLE_REGEX
#ifndef DISABLE_REGEX
#include <string>
#include <regex>
#include <algorithm>
#include <functional>
#include "badFingerprints.h"
#endif
<<<<<<< HEAD
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp

const int maxLineLength = 2048;
const char *separatorCharacters = ":?/=^";

enum FilterParseState {
  FPStart,
  FPPastWhitespace,
  FPOneBar,
  FPOneAt,
  FPData
};

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
static const int fingerprintSize = 8;
#ifndef DISABLE_REGEX
=======
#ifndef DISABLE_REGEX
static const int fingerprintSize = 8;
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
static const int fingerprintSize = 8;
#ifndef DISABLE_REGEX
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
static const char* fingerprintRegexs[2] = {
  ".*([./&_\\-=a-zA-Z0-9]{8})\\$?.*",
  "([./&_\\-=a-zA-Z0-9]{8})\\$?.*",
};
#endif

/**
 * Obtains a fingerprint for the specified filter
 */
bool getFingerprint(char *buffer, const char *input) {
#ifdef DISABLE_REGEX
  return false;
#else
  if (!input) {
    return false;
  }

  for (unsigned int  i = 0; i < sizeof(fingerprintRegexs) / sizeof(fingerprintRegexs[0]); i++) {
    std::smatch m;
    std::regex e (fingerprintRegexs[i], std::regex_constants::extended);
    std::regex_search(std::string(input), m, e);

    if (m.size() < 2 || m[1].length() == 0) {
      return false;
    }

    bool fingerprintStillGood = true;
    std::for_each(badFingerprints, badFingerprints + sizeof(badFingerprints) / sizeof(badFingerprints[0]), [&fingerprintStillGood, &m](std::string const &bad) {
      if (!fingerprintStillGood) {
        return;
      }
      std::string curMatch = m[1];
      fingerprintStillGood = fingerprintStillGood && strcmp(curMatch.c_str(), bad.c_str());
    });
    std::for_each(badSubstrings, badSubstrings + sizeof(badSubstrings) / sizeof(badSubstrings[0]), [&fingerprintStillGood, &m](std::string const &bad) {
      if (!fingerprintStillGood) {
        return;
      }
      std::string curMatch = m[1];
      fingerprintStillGood = fingerprintStillGood && !strstr(curMatch.c_str(), bad.c_str());
    });

    if (!fingerprintStillGood) {
      continue;
    }

    // Sometimes the caller only cares if there is a match and not what the match is
    if (buffer) {
      std::string curMatch = m[1];
      strcpy(buffer, curMatch.c_str());
<<<<<<< HEAD
#ifdef PERF_STATS
    cout << "extracted buffer: " << buffer << endl;
#endif
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
    }
    return true;
  }
  // This is pretty ugly but getting fingerprints is assumed to be used only when preprocessing and
  // NOT in a live environment.
  if (strlen(input) > 9) {
    // Remove first and last char
    int inputLen = strlen(input);
    char *newInput = new char[inputLen - 1];
    memcpy(newInput, input + 1, inputLen - 2);
    newInput[inputLen - 2] = '\0';
    bool foundNew = getFingerprint(buffer, newInput);
    delete[] newInput;
    return foundNew;
  }
  return false;
#endif
}

<<<<<<< HEAD
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======

>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
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

<<<<<<< HEAD
<<<<<<< HEAD
void parseFilter(const char *input, Filter &f, BloomFilter *bloomFilter, BloomFilter *exceptionBloomFilter) {
  const char *end = input;
  while (*end != '\0') end++;
  parseFilter(input, end, f, bloomFilter, exceptionBloomFilter);
}

// Not currently multithreaded safe due to the static buffer named 'data'
void parseFilter(const char *input, const char *end, Filter &f, BloomFilter *bloomFilter, BloomFilter *exceptionBloomFilter) {
=======
void parseFilter(const char *input, Filter &f) {
=======
void parseFilter(const char *input, Filter &f, BloomFilter *bloomFilter, BloomFilter *exceptionBloomFilter) {
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
  const char *end = input;
  while (*end != '\0') end++;
  parseFilter(input, end, f, bloomFilter, exceptionBloomFilter);
}

// Not currently multithreaded safe due to the static buffer named 'data'
<<<<<<< HEAD
void parseFilter(const char *input, const char *end, Filter &f) {
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
void parseFilter(const char *input, const char *end, Filter &f, BloomFilter *bloomFilter, BloomFilter *exceptionBloomFilter) {
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
  FilterParseState parseState = FPStart;
  const char *p = input;
  char data[maxLineLength];
  memset(data, 0, sizeof data);
  int i = 0;

<<<<<<< HEAD
<<<<<<< HEAD
  bool earlyBreak = false;
  while (p != end && !earlyBreak) {
=======
  while (p != end) {
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  bool earlyBreak = false;
  while (p != end && !earlyBreak) {
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
    // Check for the filter being too long
    if ((p - input) >= maxLineLength - 1) {
      return;
    }

    if (parseState == FPOneBar && *p != '|') {
      parseState = FPData;
      f.filterType = static_cast<FilterType>(f.filterType | FTLeftAnchored);
    }

<<<<<<< HEAD
<<<<<<< HEAD

=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======

>>>>>>> d3819e0... Fix no fingerprint exception filter checks
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
<<<<<<< HEAD
<<<<<<< HEAD
        earlyBreak = true;
        continue;
=======
        data[i] = '\0';
        f.data = new char[i + 1];
        memcpy(f.data, data, i + 1);
        return;

>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
        earlyBreak = true;
        continue;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp

#ifndef DISABLE_REGEX
  char fingerprintBuffer[fingerprintSize + 1];
  fingerprintBuffer[fingerprintSize] = '\0';
<<<<<<< HEAD
  if (getFingerprint(fingerprintBuffer, f.data)) {
    if (exceptionBloomFilter && f.filterType & FTException) {
      exceptionBloomFilter->add(fingerprintBuffer);
    } else if (bloomFilter) {
#ifdef PERF_STATS
      cout << "add fingerprint: " << fingerprintBuffer << endl;
#endif
      bloomFilter->add(fingerprintBuffer);
    }
  }
#endif
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  getFingerprint(fingerprintBuffer, f.data);
  if (exceptionBloomFilter && f.filterType & FTException) {
    exceptionBloomFilter->add(fingerprintBuffer);
  } else if (bloomFilter) {
    bloomFilter->add(fingerprintBuffer);
  }
#endif
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
}


ABPFilterParser::ABPFilterParser() : filters(nullptr),
  htmlRuleFilters(nullptr),
  exceptionFilters(nullptr),
  noFingerprintFilters(nullptr),
<<<<<<< HEAD
<<<<<<< HEAD
  noFingerprintExceptionFilters(nullptr),
  numFilters(0),
  numHtmlRuleFilters(0),
  numExceptionFilters(0),
  numNoFingerprintFilters(0),
  numNoFingerprintExceptionFilters(0),
  bloomFilter(nullptr),
  exceptionBloomFilter(nullptr),
  numFalsePositives(0),
  numExceptionFalsePositives(0),
  numBloomFilterSaves(0),
  numExceptionBloomFilterSaves(0) {
=======
=======
  noFingerprintExceptionFilters(nullptr),
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  numFilters(0),
  numHtmlRuleFilters(0),
  numExceptionFilters(0),
<<<<<<< HEAD
  numNoFingerprintFilters(0) {
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  numNoFingerprintFilters(0),
  numNoFingerprintExceptionFilters(0),
  bloomFilter(nullptr),
  exceptionBloomFilter(nullptr) {
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
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
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
  if (noFingerprintExceptionFilters) {
    delete[] noFingerprintExceptionFilters;
  }
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  if (noFingerprintExceptionFilters) {
    delete[] noFingerprintExceptionFilters;
  }
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  if (bloomFilter) {
    delete bloomFilter;
  }
  if (exceptionBloomFilter) {
    delete exceptionBloomFilter;
  }
<<<<<<< HEAD
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
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

<<<<<<< HEAD
#ifdef PERF_STATS
void discoverMatchingPrefix(const char *str, BloomFilter *bloomFilter, int prefixLen = fingerprintSize) {
  char sz[32];
  memset(sz, 0, sizeof(sz));
  int strLen = strlen(str);
  for (int i = 0; i < strLen - prefixLen + 1; i++) {
    if (bloomFilter->exists(str + i, prefixLen)) {
      memcpy(sz, str + i, prefixLen);
      cout <<  "Bad fingerprint: " << sz << endl;
    } else {
      // memcpy(sz, str + i, prefixLen);
      // cout <<  "Good fingerprint: " << sz;
    }
  }
}
#endif

bool ABPFilterParser::matches(const char *input, FilterOption contextOption, const char *contextDomain) {
  // We always have to check noFingerprintFilters because the bloom filter opt cannot be used for them
  bool hasMatch = hasMatchingFilters(noFingerprintFilters, numNoFingerprintFilters, input, contextOption, contextDomain);
  // If no noFingerprintFilters were hit, check the bloom filter substring fingerprint for the normal
  // filter list.   If no substring exists for the input then we know for sure the URL should not be blocked.
  if (!hasMatch && bloomFilter && !bloomFilter->substringExists(input, fingerprintSize)) {
    numBloomFilterSaves++;
    return false;
  }

  // We need to check the filters list manually because there is either a match or a false positive
  if (!hasMatch) {
    hasMatch = hasMatchingFilters(filters, numFilters, input, contextOption, contextDomain);
  }

  if (hasMatch) {
    if (hasMatchingFilters(exceptionFilters, numExceptionFilters, input, contextOption, contextDomain)) {
      return false;
    }
  }
}
#endif

bool ABPFilterParser::matches(const char *input, FilterOption contextOption, const char *contextDomain) {
  // We always have to check noFingerprintFilters because the bloom filter opt cannot be used for them
  bool hasMatch = hasMatchingFilters(noFingerprintFilters, numNoFingerprintFilters, input, contextOption, contextDomain);
  // If no noFingerprintFilters were hit, check the bloom filter substring fingerprint for the normal
  // filter list.   If no substring exists for the input then we know for sure the URL should not be blocked.
  if (!hasMatch && bloomFilter && !bloomFilter->substringExists(input, fingerprintSize)) {
    numBloomFilterSaves++;
    return false;
  }

  // We need to check the filters list manually because there is either a match or a false positive
  if (!hasMatch) {
    hasMatch = hasMatchingFilters(filters, numFilters, input, contextOption, contextDomain);
  }

  // If there's still no match after checking the block filters, then no need to try to block this
  // because there is a false positive.
  if (!hasMatch) {
    numFalsePositives++;
#ifdef PERF_STATS
    cout << "false positive for input: " << input << endl;
    discoverMatchingPrefix(input, bloomFilter);
#endif
    return false;
  }

  // If there's a matching no fingerprint exception then we can just return right away because we shouldn't block
  if (hasMatchingFilters(noFingerprintExceptionFilters, numNoFingerprintExceptionFilters, input, contextOption, contextDomain)) {
    return false;
  }

  // Now that we have a matching rule, we should check if no exception rule hits, if none hits, we should block
  if (exceptionBloomFilter && !exceptionBloomFilter->substringExists(input, fingerprintSize)) {
    numExceptionBloomFilterSaves++;
    return true;
  }

  // No bloom filter exception rule hit so it's either a false positive or a match, check to make sure
  if (hasMatchingFilters(exceptionFilters, numExceptionFilters, input, contextOption, contextDomain)) {
    // False positive on the exception filter list
    numExceptionFalsePositives++;
#ifdef PERF_STATS
    cout << "exception false positive for input: " << input << endl;
    discoverMatchingPrefix(input, exceptionBloomFilter);
#endif
    return false;
  }

  // Exception list confirmed we have an exception
  return true;
}

void ABPFilterParser::initBloomFilter(const char *buffer, int len) {
  if (bloomFilter) {
    delete bloomFilter;
  }
  if (len > 0) {
    bloomFilter = new BloomFilter(buffer, len);
  }
}
void ABPFilterParser::initExceptionBloomFilter(const char *buffer, int len) {
  if (exceptionBloomFilter) {
    delete exceptionBloomFilter;
  }
  if (len > 0) {
    exceptionBloomFilter = new BloomFilter(buffer, len);
  }
}

void ABPFilterParser::initBloomFilter(const char *buffer, int len) {
  if (bloomFilter) {
    delete bloomFilter;
  }
  if (len > 0) {
    bloomFilter = new BloomFilter(buffer, len);
  }
}
void ABPFilterParser::initExceptionBloomFilter(const char *buffer, int len) {
  if (exceptionBloomFilter) {
    delete exceptionBloomFilter;
  }
  if (len > 0) {
    exceptionBloomFilter = new BloomFilter(buffer, len);
  }
=======
bool ABPFilterParser::matches(const char *input, FilterOption contextOption, const char *contextDomain) {
  // We always have to check noFingerprintFilters because the bloom filter opt cannot be used for them
  bool hasMatch = hasMatchingFilters(noFingerprintFilters, numNoFingerprintFilters, input, contextOption, contextDomain);
  // If no noFingerprintFilters were hit, check the bloom filter substring fingerprint for the normal
  // filter list.   If no substring exists for the input then we know for sure the URL should not be blocked.
  if (!hasMatch && bloomFilter && !bloomFilter->substringExists(input, fingerprintSize)) {
    return false;
  }

  // We need to check the filters list manually because there is either a match or a false positive
  if (!hasMatch) {
    hasMatch = hasMatchingFilters(filters, numFilters, input, contextOption, contextDomain);
  }

  // If there's still no match after checking the block filters, then no need to try to block this
  // because there is a false positive.
  if (!hasMatch) {
    return false;
  }

  // If there's a matching no fingerprint exception then we can just return right away because we shouldn't block
  if (hasMatchingFilters(noFingerprintExceptionFilters, numNoFingerprintExceptionFilters, input, contextOption, contextDomain)) {
    return false;
  }

  // Now that we have a matching rule, we should check if no exception rule hits, if none hits, we should block
  if (exceptionBloomFilter && !exceptionBloomFilter->substringExists(input, fingerprintSize)) {
    return true;
  }
<<<<<<< HEAD
  return false;
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======

  // No bloom filter exception rule hit so it's either a false positive or a match, check to make sure
  if (hasMatchingFilters(exceptionFilters, numExceptionFilters, input, contextOption, contextDomain)) {
    // False positive on the exception filter list
    return false;
  }

  // Exception list confirmed we have an exception
  return true;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
}

void ABPFilterParser::initBloomFilter(const char *buffer, int len) {
  if (bloomFilter) {
    delete bloomFilter;
  }
  if (len > 0) {
    bloomFilter = new BloomFilter(buffer, len);
  }
}
void ABPFilterParser::initExceptionBloomFilter(const char *buffer, int len) {
  if (exceptionBloomFilter) {
    delete exceptionBloomFilter;
  }
  if (len > 0) {
    exceptionBloomFilter = new BloomFilter(buffer, len);
  }
}

// Parses the filter data into a few collections of filters and enables efficent querying
bool ABPFilterParser::parse(const char *input) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
#ifndef DISABLE_REGEX
  // If the user is parsing and we have regex support,
  // then we can determine the fingerprints for the bloom filter.
  // Otherwise it needs to be done manually via initBloomFilter and initExceptionBloomFilter
  if (!bloomFilter) {
    bloomFilter = new BloomFilter();
  }
  if (!exceptionBloomFilter) {
    exceptionBloomFilter = new BloomFilter();
  }
#endif

<<<<<<< HEAD
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
  const char *p = input;
  const char *lineStart = p;

  int newNumFilters = 0;
  int newNumHtmlRuleFilters = 0;
  int newNumExceptionFilters = 0;
  int newNumNoFingerprintFilters = 0;
<<<<<<< HEAD
<<<<<<< HEAD
  int newNumNoFingerprintExceptionFilters = 0;
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  int newNumNoFingerprintExceptionFilters = 0;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks

  // Parsing does 2 passes, one just to determine the type of information we'll need to setup.
  // Note that the library will be used on a variety of builds so sometimes we won't even have STL
  // So we can't use something like a vector here.
  while (true) {
    if (*p == '\n' || *p == '\0') {
      Filter f;
      parseFilter(lineStart, p, f);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
          if (getFingerprint(nullptr, f.data)) {
            newNumExceptionFilters++;
          } else {
            newNumNoFingerprintExceptionFilters++;
          }
<<<<<<< HEAD
=======
          newNumExceptionFilters++;
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
          if (getFingerprint(nullptr, f.data)) {
            newNumFilters++;
          } else {
            newNumNoFingerprintFilters++;
          }
<<<<<<< HEAD
=======
          // TODO: check if no fingerprint and if so update numNoFingerprintFilters
          newNumFilters++;
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
          break;
      }
      lineStart = p + 1;
    }

    if (*p == '\0') {
      break;
    }

    p++;
  };


<<<<<<< HEAD
#ifdef PERF_STATS
  cout << "Num no fingerprint filters: " << numNoFingerprintFilters << endl;
  cout << "Num no fingerprint exception filters: " << numNoFingerprintExceptionFilters << endl;
#endif

=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
  Filter *newFilters = new Filter[newNumFilters + numFilters];
  Filter *newHtmlRuleFilters = new Filter[newNumHtmlRuleFilters + numHtmlRuleFilters];
  Filter *newExceptionFilters = new Filter[newNumExceptionFilters + numExceptionFilters];
  Filter *newNoFingerprintFilters = new Filter[newNumNoFingerprintFilters + numNoFingerprintFilters];
  Filter *newNoFingerprintExceptionFilters = new Filter[newNumNoFingerprintExceptionFilters + numNoFingerprintExceptionFilters];

  memset(newFilters, 0, sizeof(Filter) * (newNumFilters + numFilters));
  memset(newHtmlRuleFilters, 0, sizeof(Filter) * (newNumHtmlRuleFilters + numHtmlRuleFilters));
  memset(newExceptionFilters, 0, sizeof(Filter) * (newNumExceptionFilters + numExceptionFilters));
  memset(newNoFingerprintFilters, 0, sizeof(Filter) * (newNumNoFingerprintFilters + numNoFingerprintFilters));
  memset(newNoFingerprintExceptionFilters, 0, sizeof(Filter) * (newNumNoFingerprintExceptionFilters + numNoFingerprintExceptionFilters));

  Filter *curFilters = newFilters;
  Filter *curHtmlRuleFilters = newHtmlRuleFilters;
  Filter *curExceptionFilters = newExceptionFilters;
  Filter *curNoFingerprintFilters = newNoFingerprintFilters;
  Filter *curNoFingerprintExceptionFilters = newNoFingerprintExceptionFilters;

  // If we've had a parse before copy the old data into the new data structure
  if (filters || htmlRuleFilters || exceptionFilters || noFingerprintFilters || noFingerprintExceptionFilters) {
    // Copy the old data in
    memcpy(newFilters, filters, sizeof(Filter) * numFilters);
    memcpy(newHtmlRuleFilters, htmlRuleFilters, sizeof(Filter) * numHtmlRuleFilters);
    memcpy(newExceptionFilters, exceptionFilters, sizeof(Filter) * numExceptionFilters);
    memcpy(newNoFingerprintFilters, noFingerprintFilters, sizeof(Filter) * (numNoFingerprintFilters));
    memcpy(newNoFingerprintExceptionFilters, noFingerprintExceptionFilters, sizeof(Filter) * (numNoFingerprintExceptionFilters));

    // Adjust the current pointers to be just after the copied in data
    curFilters += numFilters;
    curHtmlRuleFilters += numHtmlRuleFilters;
    curExceptionFilters += numExceptionFilters;
    curNoFingerprintFilters += numNoFingerprintFilters;
    curNoFingerprintExceptionFilters += numNoFingerprintExceptionFilters;
  }

  // And finally update with the new counts
  numFilters += newNumFilters;
  numHtmlRuleFilters += newNumHtmlRuleFilters;
  numExceptionFilters += newNumExceptionFilters;
  numNoFingerprintFilters += newNumNoFingerprintFilters;
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  numNoFingerprintExceptionFilters += newNumNoFingerprintExceptionFilters;

  // Adjust the new member list pointers
  filters = newFilters;
  htmlRuleFilters = newHtmlRuleFilters;
  exceptionFilters = newExceptionFilters;
  noFingerprintFilters = newNoFingerprintFilters;
<<<<<<< HEAD
<<<<<<< HEAD
  noFingerprintExceptionFilters = newNoFingerprintExceptionFilters;
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  noFingerprintExceptionFilters = newNoFingerprintExceptionFilters;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks

  p = input;
  lineStart = p;

  while (true) {
    if (*p == '\n' || *p == '\0') {
      Filter f;
<<<<<<< HEAD
<<<<<<< HEAD
      parseFilter(lineStart, p, f, bloomFilter, exceptionBloomFilter);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
          if (getFingerprint(nullptr, f.data)) {
            (*curExceptionFilters).swap(f);
            curExceptionFilters++;
          } else {
            (*curNoFingerprintExceptionFilters).swap(f);
            curNoFingerprintExceptionFilters++;
          }
=======
      parseFilter(lineStart, p, f);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
          (*curExceptionFilters).swap(f);
          curExceptionFilters++;
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
      parseFilter(lineStart, p, f, bloomFilter, exceptionBloomFilter);
      switch(f.filterType & FTListTypesMask) {
        case FTException:
          if (getFingerprint(nullptr, f.data)) {
            (*curExceptionFilters).swap(f);
            curExceptionFilters++;
          } else {
            (*curNoFingerprintExceptionFilters).swap(f);
            curNoFingerprintExceptionFilters++;
          }
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
          if (getFingerprint(nullptr, f.data)) {
            (*curFilters).swap(f);
            curFilters++;
          } else {
            (*curNoFingerprintFilters).swap(f);
            curNoFingerprintFilters++;
          }
<<<<<<< HEAD
=======
          // TODO: check if no fingerprint here and update noFingerprintFilters instead
          (*curFilters).swap(f);
          curFilters++;
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
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

<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
// Fills the specified buffer if specified, returns the number of characters written or needed
int serializeFilters(char * buffer, Filter *f, int numFilters) {
  char sz[256];
  int bufferSize = 0;
  for (int i = 0; i < numFilters; i++) {
    int sprintfLen = sprintf(sz, "%x,%x,%x", (int)f->filterType, (int)f->filterOption, (int)f->antiFilterOption);
    if (buffer) {
      strcpy(buffer + bufferSize, sz);
    }
    bufferSize += sprintfLen;
    // Extra null termination
    bufferSize++;

    if (f->data) {
      if (buffer) {
        strcpy(buffer + bufferSize, f->data);
      }
      bufferSize += strlen(f->data);
    }
    bufferSize++;

    if (f->domainList) {
      if (buffer) {
        strcpy(buffer + bufferSize, f->domainList);
      }
      bufferSize += strlen(f->domainList);
    }
    // Extra null termination
    bufferSize++;
    if (f->host) {
      if (buffer) {
        strcpy(buffer + bufferSize, f->host);
      }
      bufferSize += strlen(f->host);
    }
    // Extra null termination
    bufferSize++;
    f++;
  }
  return bufferSize;
}

// Returns a newly allocated buffer, caller must manually delete[] the buffer
<<<<<<< HEAD
<<<<<<< HEAD
char * ABPFilterParser::serialize(int &totalSize, bool ignoreHTMLFilters) {
  totalSize = 0;
  int adjustedNumHTMLFilters = ignoreHTMLFilters ? 0 : numHtmlRuleFilters;

  // Get the number of bytes that we'll need
  char sz[512];
  totalSize += sprintf(sz, "%x,%x,%x,%x,%x,%x,%x", numFilters, numExceptionFilters, adjustedNumHTMLFilters, numNoFingerprintFilters, numNoFingerprintExceptionFilters, bloomFilter ? bloomFilter->getByteBufferSize() : 0, exceptionBloomFilter ? exceptionBloomFilter->getByteBufferSize() : 0);
  totalSize += serializeFilters(nullptr, filters, numFilters) +
    serializeFilters(nullptr, exceptionFilters, numExceptionFilters) +
    serializeFilters(nullptr, htmlRuleFilters, adjustedNumHTMLFilters) +
    serializeFilters(nullptr, noFingerprintFilters, numNoFingerprintFilters) +
    serializeFilters(nullptr, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
  totalSize = 0;
  int adjustedNumHTMLFilters = ignoreHTMLFilters ? 0 : numHtmlRuleFilters;

  // Get the number of bytes that we'll need
  char sz[512];
  totalSize += sprintf(sz, "%x,%x,%x,%x,%x,%x,%x", numFilters, numExceptionFilters, adjustedNumHTMLFilters, numNoFingerprintFilters, numNoFingerprintExceptionFilters, bloomFilter ? bloomFilter->getByteBufferSize() : 0, exceptionBloomFilter ? exceptionBloomFilter->getByteBufferSize() : 0);
=======
char * ABPFilterParser::serialize(int &totalSize) {
=======
char * ABPFilterParser::serialize(int &totalSize, bool ignoreHTMLFilters) {
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  totalSize = 0;
  int adjustedNumHTMLFilters = ignoreHTMLFilters ? 0 : numHtmlRuleFilters;

  // Get the number of bytes that we'll need
  char sz[512];
<<<<<<< HEAD
  totalSize += sprintf(sz, "%x,%x,%x,%x,%x,%x", numFilters, numExceptionFilters, numHtmlRuleFilters, numNoFingerprintFilters, bloomFilter ? bloomFilter->getByteBufferSize() : 0, exceptionBloomFilter ? exceptionBloomFilter->getByteBufferSize() : 0);
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
  totalSize += serializeFilters(nullptr, filters, numFilters) +
    serializeFilters(nullptr, exceptionFilters, numExceptionFilters) +
    serializeFilters(nullptr, htmlRuleFilters, numHtmlRuleFilters) +
    serializeFilters(nullptr, noFingerprintFilters, numNoFingerprintFilters);
<<<<<<< HEAD
    serializeFilters(nullptr, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  totalSize += sprintf(sz, "%x,%x,%x,%x,%x,%x,%x", numFilters, numExceptionFilters, adjustedNumHTMLFilters, numNoFingerprintFilters, numNoFingerprintExceptionFilters, bloomFilter ? bloomFilter->getByteBufferSize() : 0, exceptionBloomFilter ? exceptionBloomFilter->getByteBufferSize() : 0);
  totalSize += serializeFilters(nullptr, filters, numFilters) +
    serializeFilters(nullptr, exceptionFilters, numExceptionFilters) +
    serializeFilters(nullptr, htmlRuleFilters, adjustedNumHTMLFilters) +
    serializeFilters(nullptr, noFingerprintFilters, numNoFingerprintFilters) +
    serializeFilters(nullptr, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  totalSize += bloomFilter ? bloomFilter->getByteBufferSize() : 0;
  totalSize += exceptionBloomFilter ? exceptionBloomFilter->getByteBufferSize() : 0;

  // Allocate it
  int pos = 0;
  char *buffer = new char[totalSize];
  memset(buffer, 0, totalSize);

  // And start copying stuff in
  strcpy(buffer, sz);
  pos += strlen(sz) + 1;
  pos += serializeFilters(buffer + pos, filters, numFilters);
  pos += serializeFilters(buffer + pos, exceptionFilters, numExceptionFilters);
<<<<<<< HEAD
<<<<<<< HEAD
  pos += serializeFilters(buffer + pos, htmlRuleFilters, adjustedNumHTMLFilters);
  pos += serializeFilters(buffer + pos, noFingerprintFilters, numNoFingerprintFilters);
  pos += serializeFilters(buffer + pos, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
=======
  pos += serializeFilters(buffer + pos, htmlRuleFilters, numHtmlRuleFilters);
  pos += serializeFilters(buffer + pos, noFingerprintFilters, numNoFingerprintFilters);
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  pos += serializeFilters(buffer + pos, htmlRuleFilters, adjustedNumHTMLFilters);
  pos += serializeFilters(buffer + pos, noFingerprintFilters, numNoFingerprintFilters);
  pos += serializeFilters(buffer + pos, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  if (bloomFilter) {
    memcpy(buffer + pos, bloomFilter->getBuffer(), bloomFilter->getByteBufferSize());
    pos += bloomFilter->getByteBufferSize();
  }
  if (exceptionBloomFilter) {
    memcpy(buffer + pos, exceptionBloomFilter->getBuffer(), exceptionBloomFilter->getByteBufferSize());
    pos += exceptionBloomFilter->getByteBufferSize();
  }
  return buffer;
}

// Fills the specified buffer if specified, returns the number of characters written or needed
int deserializeFilters(char *buffer, Filter *f, int numFilters) {
  int pos = 0;
  for (int i = 0; i < numFilters; i++) {
    f->borrowedData = true;
    sscanf(buffer + pos, "%x,%x,%x", &f->filterType, &f->filterOption, &f->antiFilterOption);
    pos += strlen(buffer + pos) + 1;

    if (*(buffer + pos) == '\0') {
      f->data = nullptr;
    } else {
      f->data = buffer + pos;
      pos += strlen(f->data);
    }
    pos++;

   if (*(buffer + pos) == '\0') {
      f->domainList = nullptr;
    } else {
      f->domainList = buffer + pos;
      pos += strlen(f->domainList);
    }
    pos++;

   if (*(buffer + pos) == '\0') {
      f->host = nullptr;
    } else {
      f->host = buffer + pos;
      pos += strlen(f->host);
    }
    pos++;
    f++;
  }
  return pos;
}

void ABPFilterParser::deserialize(char *buffer) {
  int bloomFilterSize = 0, exceptionBloomFilterSize = 0;
  int pos = 0;
<<<<<<< HEAD
<<<<<<< HEAD
  sscanf(buffer + pos, "%x,%x,%x,%x,%x,%x,%x", &numFilters, &numExceptionFilters, &numHtmlRuleFilters, &numNoFingerprintFilters, &numNoFingerprintExceptionFilters, &bloomFilterSize, &exceptionBloomFilterSize);
=======
  sscanf(buffer + pos, "%x,%x,%x,%x,%x,%x", &numFilters, &numExceptionFilters, &numHtmlRuleFilters, &numNoFingerprintFilters, &bloomFilterSize, &exceptionBloomFilterSize);
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  sscanf(buffer + pos, "%x,%x,%x,%x,%x,%x,%x", &numFilters, &numExceptionFilters, &numHtmlRuleFilters, &numNoFingerprintFilters, &numNoFingerprintExceptionFilters, &bloomFilterSize, &exceptionBloomFilterSize);
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  pos += strlen(buffer + pos) + 1;

  filters = new Filter[numFilters];
  exceptionFilters = new Filter[numExceptionFilters];
  htmlRuleFilters = new Filter[numHtmlRuleFilters];
  noFingerprintFilters = new Filter[numNoFingerprintFilters];
<<<<<<< HEAD
<<<<<<< HEAD
  noFingerprintExceptionFilters = new Filter[numNoFingerprintExceptionFilters];
=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  noFingerprintExceptionFilters = new Filter[numNoFingerprintExceptionFilters];
>>>>>>> d3819e0... Fix no fingerprint exception filter checks

  pos += deserializeFilters(buffer + pos, filters, numFilters);
  pos += deserializeFilters(buffer + pos, exceptionFilters, numExceptionFilters);
  pos += deserializeFilters(buffer + pos, htmlRuleFilters, numHtmlRuleFilters);
  pos += deserializeFilters(buffer + pos, noFingerprintFilters, numNoFingerprintFilters);
<<<<<<< HEAD
<<<<<<< HEAD
  pos += deserializeFilters(buffer + pos, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);

  initBloomFilter(buffer + pos, bloomFilterSize);
  pos += bloomFilterSize;
  initExceptionBloomFilter(buffer + pos, exceptionBloomFilterSize);
  pos += exceptionBloomFilterSize;
}
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
=======
  pos += deserializeFilters(buffer + pos, noFingerprintExceptionFilters, numNoFingerprintExceptionFilters);
>>>>>>> d3819e0... Fix no fingerprint exception filter checks

  initBloomFilter(buffer + pos, bloomFilterSize);
  pos += bloomFilterSize;
  initExceptionBloomFilter(buffer + pos, exceptionBloomFilterSize);
  pos += exceptionBloomFilterSize;
}
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
