#pragma once

#include "filter.h"
<<<<<<< HEAD
<<<<<<< HEAD
#include "BloomFilter.h"
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
#include "BloomFilter.h"
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp

class ABPFilterParser
{
public:
  ABPFilterParser();
  ~ABPFilterParser();

  bool parse(const char *input);
  bool matches(const char *input, FilterOption contextOption = FONoFilterOption, const char *contextDomain = nullptr);
<<<<<<< HEAD
<<<<<<< HEAD
  // Serializes a the parsed data and bloom filter data into a single buffer.
  // The returned buffer should be deleted.
  char * serialize(int &size, bool ignoreHTMLFilters = true);
  // Deserializes the buffer, a size is not needed since a serialized buffer is self described
  void deserialize(char *);
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  // Serializes a the parsed data and bloom filter data into a single buffer.
  // The returned buffer should be deleted.
  char * serialize(int &size, bool ignoreHTMLFilters = true);
  // Deserializes the buffer, a size is not needed since a serialized buffer is self described
  void deserialize(char *);
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp

  Filter *filters;
  Filter *htmlRuleFilters;
  Filter *exceptionFilters;
  Filter *noFingerprintFilters;
<<<<<<< HEAD
<<<<<<< HEAD
  Filter *noFingerprintExceptionFilters;
=======
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
  Filter *noFingerprintExceptionFilters;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks
  int numFilters;
  int numHtmlRuleFilters;
  int numExceptionFilters;
  int numNoFingerprintFilters;
<<<<<<< HEAD
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
=======
  int numNoFingerprintExceptionFilters;
>>>>>>> d3819e0... Fix no fingerprint exception filter checks

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
<<<<<<< HEAD
void parseFilter(const char *input, const char *end, Filter&);
void parseFilter(const char *input, Filter&);
>>>>>>> eb19c89... Add Ad Block Plus C++ filter library and JNI integration code
=======
void parseFilter(const char *input, const char *end, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
void parseFilter(const char *input, Filter&, BloomFilter *bloomFilter = nullptr, BloomFilter *exceptionBloomFilter = nullptr);
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
bool isSeparatorChar(char c);
int findFirstSeparatorChar(const char *input, const char *end);
