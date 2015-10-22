#include "filter.h"
#include "ABPFilterParser.h"
#include <string.h>

#define DISABLE_REGEX

#ifndef DISABLE_REGEX
#include <string>
#include <regex>
#endif

using namespace std;

Filter::Filter() :
  filterType(FTNoFilterType),
  filterOption(FONoFilterOption),
  antiFilterOption(FONoFilterOption),
  data(nullptr),
  domainList(nullptr),
  host(nullptr) {
}

Filter::~Filter() {
  if (data) {
    delete[] data;
  }
  if (domainList) {
    delete[] domainList;
  }
  if (host) {
    delete[] host;
  }
}

void Filter::swap(Filter &other) {
  FilterType tempFilterType = filterType;
  FilterOption tempFilterOption = filterOption;
  FilterOption tempAntiFilterOption = antiFilterOption;
  char *tempData = data;
  char *tempDomainList = domainList;
  char *tempHost = host;

  filterType = other.filterType;
  filterOption = other.filterOption;
  antiFilterOption = other.antiFilterOption;
  data = other.data;
  domainList = other.domainList;
  host = other.host;

  other.filterType = tempFilterType;
  other.filterOption = tempFilterOption;
  other.antiFilterOption = tempAntiFilterOption;
  other.data = tempData;
  other.domainList = tempDomainList;
  other.host = tempHost;
}

/**
 * Finds the host within the passed in URL and returns its length
 */
const char * getUrlHost(const char *input, int &len) {
  const char *p = input;
  while (*p != '\0' && *p != ':') {
    p++;
  }
  if (*p != '\0') {
    p++;
    while (*p != '\0' && *p == '/') {
      p++;
    }
  }
  const char *q = p;
  while (*q != '\0') {
    q++;
  }
  len = findFirstSeparatorChar(p, q);
  if (len == -1) {
    len = strlen(p);
  }
  return p;
}

bool isDomain(const char *input, int len, const char *domain, bool anti) {
  const char *p = input;
  if (anti) {
    if (len >= 1 && p[0] != '~') {
      return false;
    } else {
      len--;
      p++;
    }
  }
  return !memcmp(p, domain, len);
}

bool Filter::containsDomain(const char *domain, bool anti) const {
  if (!domainList) {
    return false;
  }

  int startOffset = 0;
  int len = 0;
  const char *p = domainList;
  while (*p != '\0') {
    if (*p == '|') {
      if (isDomain(domainList + startOffset, len, domain, anti)) {
        return true;
      }
      startOffset += len + 1;
      len = -1;
    }
    p++;
    len++;
  }
  return isDomain(domainList + startOffset, len, domain, anti);
}

int Filter::getDomainCount(bool anti) const {
  if (!domainList || domainList[0] == '\0') {
    return 0;
  }

  int count = 0;
  int startOffset = 0;
  int len = 0;
  const char *p = domainList;
  while (*p != '\0') {
    if (*p == '|') {
      if (*(domainList + startOffset) == '~' && anti) {
        count++;
      }
      else if (*(domainList + startOffset) != '~' && !anti) {
        count++;
      }
      startOffset = len + 1;
      len = -1;
    }
    p++;
    len++;
  }

  if (*(domainList + startOffset) == '~' && anti) {
    count++;
  }
  else if (*(domainList + startOffset) != '~' && !anti) {
    count++;
  }
  return count;
}

void Filter::parseOption(const char *input, int len) {
  FilterOption *pFilterOption = &filterOption;
  const char *pStart = input;
  if (input[0] == '~') {
    pFilterOption = &antiFilterOption;
    pStart++;
    len --;
  }

  if (len >= 7 && !memcmp(pStart, "domain=", 7)) {
    len -= 7;
    domainList = new char[len + 1];
    domainList[len] = '\0';
    memcpy(domainList, pStart + 7, len);
  } else if (!memcmp(pStart, "script", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOScript);
  } else if (!memcmp(pStart, "image", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOImage);
  } else if (!memcmp(pStart, "stylesheet", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOStylesheet);
  } else if (!memcmp(pStart, "object", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOObject);
  } else if (!memcmp(pStart, "xmlhttprequest", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOXmlHttpRequest);
  } else if (!memcmp(pStart, "object-subrequest", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOObjectSubrequest);
  } else if (!memcmp(pStart, "subdocument", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOSubdocument);
  } else if (!memcmp(pStart, "document", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FODocument);
  } else if (!memcmp(pStart, "xbl", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOXBL);
  } else if (!memcmp(pStart, "collapse", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOCollapse);
  } else if (!memcmp(pStart, "donottrack", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FODoNotTrack);
  } else if (!memcmp(pStart, "other", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOOther);
  } else if (!memcmp(pStart, "elemhide", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOElemHide);
  } else if (!memcmp(pStart, "third-party", len)) {
    *pFilterOption = static_cast<FilterOption>(*pFilterOption | FOThirdParty);
  }
  // Otherwise just ignore the option, maybe something new we don't support yet
}

void Filter::parseOptions(const char *input) {
  filterOption = FONoFilterOption;
  antiFilterOption = FONoFilterOption;
  int startOffset = 0;
  int len = 0;
  const char *p = input;
  while (*p != '\0' && *p != '\n') {
    if (*p == ',') {
      parseOption(input + startOffset, len);
      startOffset += len + 1;
      len = -1;
    }
    p++;
    len++;
  }
  parseOption(input + startOffset, len);
}

bool endsWith(const char *input, const char *sub, int inputLen, int subLen) {
  if (subLen > inputLen) {
    return false;
  }

  int startCheckPos = inputLen - subLen;
  const char *p = input + startCheckPos;
  const char *q = sub;
  while (q != sub + subLen) {
    if (*(p++) != *(q++)) {
      return false;
    }
  }
  return true;
}

bool isThirdPartyHost(const char *baseContextHost, int baseContextHostLen, const char *testHost, int testHostLen) {
  if (!endsWith(testHost, baseContextHost, testHostLen, baseContextHostLen)) {
    return true;
  }

  char c = testHost[testHostLen - baseContextHostLen - 1];
  return c != '.' && testHostLen != baseContextHostLen;
}

// Determines if there's a match based on the options, this doesn't
// mean that the filter rule should be accepted, just that the filter rule
// should be considered given the current context.
// By specifying context params, you can filter out the number of rules which are
// considered.
bool Filter::matchesOptions(const char *input, FilterOption context, const char *contextDomain) {
  // Maybe the user of the library can't determine a context because they're
  // blocking a the HTTP level, don't block here because we don't have enough
  // information
  if (context != FONoFilterOption) {
    if ((filterOption & ~FOThirdParty) != FONoFilterOption && !(filterOption & context)) {
      return false;
    }

    if ((antiFilterOption & ~FOThirdParty) != FONoFilterOption && (antiFilterOption & context)) {
      return false;
    }
  }

  // Domain options check
  if (domainList && contextDomain) {
    int bufSize = strlen(domainList) + 1;
    char *shouldBlockDomains = new char[bufSize];
    char *shouldSkipDomains = new char[bufSize];
    memset(shouldBlockDomains, 0, bufSize);
    memset(shouldSkipDomains, 0, bufSize);
    filterDomainList(domainList, shouldBlockDomains, contextDomain, false);
    filterDomainList(domainList, shouldSkipDomains, contextDomain, true);

    int leftOverBlocking = getLeftoverDomainCount(shouldBlockDomains, shouldSkipDomains);
    int leftOverSkipping = getLeftoverDomainCount(shouldSkipDomains, shouldBlockDomains);
    int shouldBlockDomainsLen = strlen(shouldBlockDomains);
    int shouldSkipDomainsLen = strlen(shouldSkipDomains);

    if ((shouldBlockDomainsLen == 0 && getDomainCount() != 0) ||
        (shouldBlockDomainsLen > 0 && leftOverBlocking == 0) ||
        (shouldSkipDomainsLen > 0 && leftOverSkipping > 0)) {
      return false;
    }
  }

  // If we're in the context of third-party site, then consider third-party option checks
  if (context & (FOThirdParty | FONotThirdParty)) {
    if ((filterOption & FOThirdParty) && host) {
      int inputHostLen;
      const char *inputHost = getUrlHost(input, inputHostLen);
      bool inputHostIsThirdParty = isThirdPartyHost(host, strlen(host), inputHost, inputHostLen);
      if (inputHostIsThirdParty || !(context & FOThirdParty)) {
        return false;
      }
    }
  }

  return true;
}


const char * getNextPos(const char *input, char separator) {
  const char *p = input;
  while (*p != '\0' && *p != separator) {
    p++;
  }
  return p;
}

int indexOf(const char *source, const char *filterPartStart, const char *filterPartEnd) {
  const char *s = source;
  const char *fStart = filterPartStart;
  const char *notCheckedSource = source;

  while (*s != '\0') {
    if (fStart == filterPartEnd) {
      return s - source - (filterPartEnd - filterPartStart);
    }
    if (*s != *fStart) {
      notCheckedSource++;
      s = notCheckedSource;
      fStart = filterPartStart;
      continue;
    }

    fStart++;
    s++;
  }

  if (fStart == filterPartEnd) {
    return s - source - (filterPartEnd - filterPartStart);
  }

  return -1;
}

/**
 * Similar to str1.indexOf(filter, startingPos) but with
 * extra consideration to some ABP filter rules like ^.
 */
int indexOfFilter(const char* input, const char *filterPosStart, const char *filterPosEnd) {
  bool prefixedSeparatorChar = false;
  int filterLen = filterPosEnd - filterPosStart;
  int inputLen = strlen(input);
  int index = 0;
  int beginIndex = -1;
  if (filterLen > inputLen) {
    return -1;
  }

  const char *filterPartStart = filterPosStart;
  const char *filterPartEnd = getNextPos(filterPosStart, '^');
  if (filterPartEnd - filterPosEnd > 0) {
    filterPartEnd = filterPosEnd;
  }

  while (*(input + index) != '\0') {
    if (filterPartStart == filterPartEnd && filterPartStart != filterPosStart) {
      prefixedSeparatorChar = true;
    }
    int lastIndex = index;
    index = indexOf(input + index, filterPartStart, filterPartEnd);
    if (index == -1) {
      return -1;
    }
    index += lastIndex;
    if (beginIndex == -1) {
      beginIndex = index;
    }

    index += (filterPartEnd - filterPartStart);

    if (prefixedSeparatorChar) {
      char testChar = *(input + index + (filterPartEnd - filterPartStart));
      if (!isSeparatorChar(testChar)) {
        return -1;
      }
    }

    if (*filterPartEnd == '\0') {
      break;
    }
    const char *temp = getNextPos(filterPartEnd + 1, '^');
    filterPartStart = filterPartEnd + 1;
    filterPartEnd = temp;
    prefixedSeparatorChar = false;
    if (filterPartEnd - filterPosEnd > 0) {
      break;
    }
  }

  return beginIndex;
}

bool Filter::matches(const char *input, FilterOption contextOption, const char *contextDomain) {
  if (!matchesOptions(input, contextOption, contextDomain)) {
    return false;
  }

  if (!data) {
    return false;
  }
  int dataLen = strlen(data);
  int inputLen = strlen(input);

  // Check for a regex match
  if (filterType & FTRegex) {
#ifndef DISABLE_REGEX
    std::smatch m;
    std::regex e (data);
    return std::regex_search(std::string(input), m, e);
#else
    return false;
#endif
  }

  // Check for both left and right anchored
  if ((filterType & FTLeftAnchored) && (filterType & FTRightAnchored)) {
    return !strcmp(data, input);
  }

  // Check for right anchored
  if (filterType & FTRightAnchored) {
    if (dataLen > inputLen) {
      return false;
    }

    return !strcmp(input + (inputLen - dataLen), data);
  }

  // Check for left anchored
  if (filterType & FTLeftAnchored) {
    return !strncmp(data, input, dataLen);
  }

  // Check for domain name anchored
  if (filterType & FTHostAnchored) {

    const char *filterPartEnd = data;
    while (*filterPartEnd != '\0') {
      filterPartEnd++;
    }
    int currentHostLen;
    const char *currentHost = getUrlHost(input, currentHostLen);
    int hostLen = strlen(host);
    return !isThirdPartyHost(host, hostLen, currentHost, currentHostLen) &&
      indexOfFilter(input, data, filterPartEnd) != -1;
  }

  // Wildcard match comparison
  const char *filterPartStart = data;
  const char *filterPartEnd = getNextPos(data, '*');
  int index = 0;
  while (filterPartStart != filterPartEnd) {
    int filterPartLen = filterPartEnd - filterPartStart;
    int newIndex = indexOfFilter(input + index, filterPartStart, filterPartEnd);
    if (newIndex == -1) {
      return false;
    }
    newIndex += index;

    if (*filterPartEnd == '\0') {
      break;
    }
    const char *temp = getNextPos(filterPartEnd + 1, '*');
    filterPartStart = filterPartEnd + 1;
    filterPartEnd = temp;
    index = newIndex + filterPartLen;
    if (*(input + newIndex) == '\0') {
      break;
    }
  }

  return true;
}

void Filter::filterDomainList(const char *domainList, char *destBuffer, const char *contextDomain, bool anti) {
  if (!domainList) {
    return;
  }

  char *curDest = destBuffer;
  int contextDomainLen = strlen(contextDomain);
  int startOffset = 0;
  int len = 0;
  const char *p = domainList;
  while (true) {
    if (*p == '|' || *p == '\0') {

      const char *domain = domainList + startOffset;
      if (!isThirdPartyHost(domain[0] == '~' ? domain + 1 : domain, domain[0] == '~' ? len -1 : len, contextDomain, contextDomainLen)) {
        // We're only considering domains, not anti domains
        if (!anti && len > 0 && *domain != '~') {
          memcpy(curDest, domain, len);
          curDest[len + 1] = '|';
          curDest[len + 2] = '\0';
        } else if (anti && len > 0 && *domain == '~') {
          memcpy(curDest, domain + 1, len - 1);
          curDest[len] = '|';
          curDest[len + 1] = '\0';
        }
      }

      startOffset += len + 1;
      len = -1;
    }

    if (*p == '\0') {
      break;
    }
    p++;
    len++;
  }
}

bool isEveryDomainThirdParty(const char *shouldSkipDomains, const char *shouldBlockDomain, int shouldBlockDomainLen) {
  bool everyDomainThirdParty = true;
  if (!shouldSkipDomains) {
    return false;
  }

  int startOffset = 0;
  int len = 0;
  const char *p = shouldSkipDomains;
  while (true) {
    if (*p == '|' || *p == '\0') {

      const char *domain = shouldSkipDomains + startOffset;
      if (*domain == '~') {
        everyDomainThirdParty = everyDomainThirdParty &&
          isThirdPartyHost(shouldBlockDomain, shouldBlockDomainLen, domain + 1, len - 1);
      } else {
        everyDomainThirdParty = everyDomainThirdParty &&
          isThirdPartyHost(shouldBlockDomain, shouldBlockDomainLen, domain, len);
      }

      startOffset += len + 1;
      len = -1;
    }

    if (*p == '\0') {
      break;
    }
    p++;
    len++;
  }

  return everyDomainThirdParty;
}

int Filter::getLeftoverDomainCount(const char *shouldBlockDomains, const char *shouldSkipDomains) {
  int leftOverBlocking = 0;

  if (strlen(shouldBlockDomains) == 0) {
    return 0;
  }

  int startOffset = 0;
  int len = 0;
  const char *p = domainList;
  while (true) {
    if (*p == '|' || *p == '\0') {
      const char *domain = domainList + startOffset;
      if (*domain == '~') {
        if (isEveryDomainThirdParty(shouldSkipDomains, domain + 1, len - 1)) {
          leftOverBlocking++;
        }
      } else {
        if (isEveryDomainThirdParty(shouldSkipDomains, domain, len)) {
          leftOverBlocking++;
        }
      }

      startOffset += len + 1;
      len = -1;
    }

    if (*p == '\0') {
      break;
    }
    p++;
    len++;
  }

  return leftOverBlocking;
}
