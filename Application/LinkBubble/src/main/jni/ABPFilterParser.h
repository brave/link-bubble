#pragma once

#include "filter.h"
#include "BloomFilter.h"

class ABPFilterParser
{
public:
  ABPFilterParser();
  ~ABPFilterParser();

  bool parse(const char *input);
  bool matches(const char *input, FilterOption contextOption = FONoFilterOption, const char *contextDomain = nullptr);
  // Serializes a the parsed data and bloom filter data into a single buffer.
  // The returned buffer should be deleted.
  char * serialize(int &size, bool ignoreHTMLFilters = true);
  // Deserializes the buffer, a size is not needed since a serialized buffer is self described
  void deserialize(char *);

  Filter *filters;
  Filter *htmlRuleFilters;
  Filter *exceptionFilters;
  Filter *noFingerprintFilters;
  Filter *noFingerprintExceptionFilters;
  int numFilters;
  int numHtmlRuleFilters;
  int numExceptionFilters;
  int numNoFingerprintFilters;
  int numNoFingerprintExceptionFilters;

  BloomFilter *bloomFilter;
  BloomFilter *exceptionBloomFilter;
protected:
  // Determines if a passed in array of filter pointers matches for any of the input
  bool hasMatchingFilters(Filter *filter, int &numFilters, const char *input, FilterOption contextOption, const char *contextDomain);
  void initBloomFilter(const char *buffer, int len);
  void initExceptionBloomFilter(const char *buffer, int len);
};

extern const char *separatorCharacters;
void parseFilter(const char *input, const char *end, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
void parseFilter(const char *input, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
bool isSeparatorChar(char c);
int findFirstSeparatorChar(const char *input, const char *end);
