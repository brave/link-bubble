#include "BloomFilter.h"
#include <string.h>

<<<<<<< HEAD
<<<<<<< HEAD
HashFn defaultHashFns[5] = {HashFn(13), HashFn(17), HashFn(31), HashFn(41), HashFn(53)};
=======
HashFn defaultHashFns[3] = {HashFn(2), HashFn(3), HashFn(5)};
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
HashFn defaultHashFns[5] = {HashFn(13), HashFn(17), HashFn(31), HashFn(41), HashFn(53)};
>>>>>>> 5a3b73f... Various tweaks to increase perf of bloom filter / rabin-karp hashing

using namespace std;

BloomFilter::BloomFilter(unsigned int bitsPerElement, unsigned int estimatedNumElements, HashFn *hashFns, int numHashFns) :
    hashFns(nullptr), numHashFns(0), byteBufferSize(0), buffer(nullptr) {
  this->hashFns = hashFns;
  this->numHashFns = numHashFns;
<<<<<<< HEAD
<<<<<<< HEAD
  lastHashes = new uint64_t[numHashFns];
=======
  lastHashes = new unsigned int[numHashFns];
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  lastHashes = new uint64_t[numHashFns];
>>>>>>> 5a3b73f... Various tweaks to increase perf of bloom filter / rabin-karp hashing
  byteBufferSize = bitsPerElement * estimatedNumElements / 8 + 1;
  bitBufferSize = byteBufferSize * 8;
  buffer = new char[byteBufferSize];
  memset(buffer, 0, byteBufferSize);
}

// Constructs a BloomFilter by copying the specified buffer and number of bytes
BloomFilter::BloomFilter(const char *buffer, int byteBufferSize, HashFn *hashFns, int numHashFns) :
    hashFns(nullptr), numHashFns(0), byteBufferSize(0), buffer(nullptr) {
  this->hashFns = hashFns;
  this->numHashFns = numHashFns;
<<<<<<< HEAD
<<<<<<< HEAD
  lastHashes = new uint64_t[numHashFns];
=======
  lastHashes = new unsigned int[numHashFns];
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
  lastHashes = new uint64_t[numHashFns];
>>>>>>> 5a3b73f... Various tweaks to increase perf of bloom filter / rabin-karp hashing
  this->byteBufferSize = byteBufferSize;
  bitBufferSize = byteBufferSize * 8;
  this->buffer = new char[byteBufferSize];
  memcpy(this->buffer, buffer, byteBufferSize);
}

BloomFilter::~BloomFilter() {
  if (buffer) {
    delete[] buffer;
  }
  if (lastHashes) {
    delete[] lastHashes;
  }
}

<<<<<<< HEAD
<<<<<<< HEAD
void BloomFilter::print() {
}

=======
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
void BloomFilter::print() {
}

>>>>>>> 5a3b73f... Various tweaks to increase perf of bloom filter / rabin-karp hashing
void BloomFilter::setBit(unsigned int bitLocation) {
  buffer[bitLocation / 8] |= 1 << bitLocation % 8;
}

bool BloomFilter::isBitSet(unsigned int bitLocation) {
  return !!(buffer[bitLocation / 8] & 1 << bitLocation % 8);
}

void BloomFilter::add(const char *input, int len) {
  for (int j = 0; j < numHashFns; j++) {
    setBit(hashFns[j](input, len) % bitBufferSize);
  }
}

void BloomFilter::add(const char *sz) {
  add(sz, strlen(sz));
}

bool BloomFilter::exists(const char *input, int len) {
  bool allSet = true;
  for (int j = 0; j < numHashFns; j++) {
    allSet = allSet && isBitSet(hashFns[j](input, len) % bitBufferSize);
  }
  return allSet;
}

bool BloomFilter::exists(const char *sz) {
  return exists(sz, strlen(sz));
}

<<<<<<< HEAD
<<<<<<< HEAD
void BloomFilter::getHashesForCharCodes(const char *input, int inputLen, uint64_t *lastHashes, uint64_t *newHashes, unsigned char lastCharCode) {
=======
void BloomFilter::getHashesForCharCodes(const char *input, int inputLen, unsigned int *lastHashes, unsigned int *newHashes, unsigned char lastCharCode) {
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
=======
void BloomFilter::getHashesForCharCodes(const char *input, int inputLen, uint64_t *lastHashes, uint64_t *newHashes, unsigned char lastCharCode) {
>>>>>>> 5a3b73f... Various tweaks to increase perf of bloom filter / rabin-karp hashing
  for (int i = 0; i < numHashFns; i++) {
    if (lastHashes) {
      *(newHashes + i) = hashFns[i](input, inputLen, lastCharCode, *(lastHashes+i));
    } else {
      *(newHashes + i) = hashFns[i](input, inputLen);
    }
  }
}

bool BloomFilter::substringExists(const char *data, int dataLen, int substringLength) {
  unsigned char lastCharCode = 0;
  for (int i = 0; i < dataLen - substringLength + 1; i++) {
    getHashesForCharCodes(data + i, substringLength, i == 0 ? nullptr : lastHashes, lastHashes, lastCharCode);
    bool allSet = true;
    for (int j = 0; j < numHashFns; j++) {
      allSet = allSet && isBitSet(lastHashes[j] % bitBufferSize);
    }
    if (allSet) {
      return true;
    }
    lastCharCode = data[i];
  }
  return false;
}

bool BloomFilter::substringExists(const char *data, int substringLength) {
  return substringExists(data, strlen(data), substringLength);
}
