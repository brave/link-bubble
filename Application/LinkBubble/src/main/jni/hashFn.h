#pragma once

// Functor for a hashing function
// Implements a Rabin fingerprint hash function
class HashFn {
public:
  // Initialize a HashFn with the prime p which is used as the base of the Rabin fingerprint algorithm
  HashFn(int p) {
    this->p = p;
  }

<<<<<<< HEAD
  virtual uint64_t operator()(const char *input, int len, unsigned char lastCharCode, uint64_t lastHash) {
=======
  virtual int operator()(const char *input, int len, unsigned char lastCharCode, unsigned int lastHash) {
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
    // See the abracadabra example: https://en.wikipedia.org/wiki/Rabin%E2%80%93Karp_algorithm
    return (lastHash - lastCharCode * pow(p, len - 1)) * p + input[len - 1];
  }

<<<<<<< HEAD
  virtual uint64_t operator()(const char *input, int len) {
    uint64_t total = 0;
=======
  virtual int operator()(const char *input, int len) {
    int total = 0;
>>>>>>> d4fb680... Update ABPFilterParser w/ BloomFilter + Rabin-Karp
    for (int i = 0; i < len; i++) {
      total += input[i] * pow(p, len - i - 1);
    }
    return total;
  }

private:
  int p;
};
