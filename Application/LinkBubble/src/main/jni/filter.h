#pragma once

#include "base.h"

enum FilterType {
  FTNoFilterType = 0,
  FTRegex = 01,
  FTElementHiding = 02,
  FTElementHidingException = 04,
  FTHostAnchored = 010,
  FTLeftAnchored = 020,
  FTRightAnchored = 040,
  FTComment = 0100,
  FTException = 0200,
  FTEmpty = 0400,
  FTListTypesMask = FTException|FTElementHiding|FTElementHidingException|FTEmpty|FTComment,
};

enum FilterOption {
  FONoFilterOption = 0,
  FOScript = 01,
  FOImage = 02,
  FOStylesheet = 04,
  FOObject = 010,
  FOXmlHttpRequest = 020,
  FOObjectSubrequest = 040,
  FOSubdocument = 0100,
  FODocument = 0200,
  FOOther = 0400,
  FOXBL = 01000,
  FOCollapse = 02000,
  FODoNotTrack = 04000,
  FOElemHide = 010000,
  FOThirdParty = 020000,
  // both options because the not present case is reated as unknown
  FONotThirdParty = 040000
};

class Filter {
friend class ABPFilterParser;
public:
  Filter();
  ~Filter();

  // Swaps the data members for 'this' and the passed in filter
  void swap(Filter&f);

  // Checks to see if any filter matches the input but does not match any exception rule
  bool matches(const char *input, FilterOption contextOption = FONoFilterOption, const char *contextDomain = nullptr);

  // Checks to see if the filter options match for the passed in data
  bool matchesOptions(const char *input, FilterOption contextOption, const char *contextDomain = nullptr);

  void parseOptions(const char *input);
  bool containsDomain(const char *domain, bool anti = false) const;
  int getDomainCount(bool anti = false) const;

  // Holds true if the filter should not free memory because for example it was loaded
  // from a large buffer somewhere else via the serialize and deserialize functions.
  bool borrowedData;

  FilterType filterType;
  FilterOption filterOption;
  FilterOption antiFilterOption;
  char *data;
  char *domainList;
  char *host;

protected:
  // Filters the domain list down to what's applicable for the context domain
  void filterDomainList(const char *domainList, char *destBuffer, const char *contextDomain, bool anti);
  // Checks for what is not excluded by the opposite list
  int getLeftoverDomainCount(const char *shouldBlockDomains, const char *shouldSkipDomains);

  // Parses a single option
  void parseOption(const char *input, int len);
};
