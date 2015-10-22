#pragma once

#include "filter.h"
<<<<<<< HEAD
#include "BloomFilter.h"
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code

class ABPFilterParser
{
public:
  ABPFilterParser();
  ~ABPFilterParser();

  bool parse(const char *input);
  bool matches(const char *input, FilterOption contextOption = FONoFilterOption, const char *contextDomain = nullptr);
<<<<<<< HEAD
  // Serializes a the parsed data and bloom filter data into a single buffer.
  // The returned buffer should be deleted.
  char * serialize(int &size, bool ignoreHTMLFilters = true);
  // Deserializes the buffer, a size is not needed since a serialized buffer is self described
  void deserialize(char *);
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code

  Filter *filters;
  Filter *htmlRuleFilters;
  Filter *exceptionFilters;
  Filter *noFingerprintFilters;
<<<<<<< HEAD
  Filter *noFingerprintExceptionFilters;
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
  int numFilters;
  int numHtmlRuleFilters;
  int numExceptionFilters;
  int numNoFingerprintFilters;
<<<<<<< HEAD
  int numNoFingerprintExceptionFilters;

  BloomFilter *bloomFilter;
  BloomFilter *exceptionBloomFilter;

  // Stats kept for matching
  unsigned int numFalsePositives;
  unsigned int numExceptionFalsePositives;
  unsigned int numBloomFilterSaves;
  unsigned int numExceptionBloomFilterSaves;
protected:
  // Determines if a passed in array of filter pointers matches for any of the input
  bool hasMatchingFilters(Filter *filter, int &numFilters, const char *input, FilterOption contextOption, const char *contextDomain);
  void initBloomFilter(const char *buffer, int len);
  void initExceptionBloomFilter(const char *buffer, int len);
};

extern const char *separatorCharacters;
void parseFilter(const char *input, const char *end, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
void parseFilter(const char *input, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
=======

protected:
  // Determines if a passed in array of filter pointers matches for any of the input
  bool hasMatchingFilters(Filter *filter, int &numFilters, const char *input, FilterOption contextOption, const char *contextDomain);

};

extern const char *separatorCharacters;
void parseFilter(const char *input, const char *end, Filter&);
void parseFilter(const char *input, Filter&);
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
bool isSeparatorChar(char c);
int findFirstSeparatorChar(const char *input, const char *end);
