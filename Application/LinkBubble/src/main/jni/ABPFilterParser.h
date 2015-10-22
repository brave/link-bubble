#pragma once

#include "filter.h"

class ABPFilterParser
{
public:
  ABPFilterParser();
  ~ABPFilterParser();

  bool parse(const char *input);
  bool matches(const char *input, FilterOption contextOption = FONoFilterOption, const char *contextDomain = nullptr);

  Filter *filters;
  Filter *htmlRuleFilters;
  Filter *exceptionFilters;
  Filter *noFingerprintFilters;
  int numFilters;
  int numHtmlRuleFilters;
  int numExceptionFilters;
  int numNoFingerprintFilters;

protected:
  // Determines if a passed in array of filter pointers matches for any of the input
  bool hasMatchingFilters(Filter *filter, int &numFilters, const char *input, FilterOption contextOption, const char *contextDomain);

};

extern const char *separatorCharacters;
void parseFilter(const char *input, const char *end, Filter&);
void parseFilter(const char *input, Filter&);
bool isSeparatorChar(char c);
int findFirstSeparatorChar(const char *input, const char *end);
