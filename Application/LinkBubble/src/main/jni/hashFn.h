#pragma once

// Functor for a hashing function
// Implements a Rabin fingerprint hash function
class HashFn {
public:
  // Initialize a HashFn with the prime p which is used as the base of the Rabin fingerprint algorithm
  HashFn(int p) {
    this->p = p;
  }

  virtual int operator()(const char *input, int len, unsigned char lastCharCode, unsigned int lastHash) {
    // See the abracadabra example: https://en.wikipedia.org/wiki/Rabin%E2%80%93Karp_algorithm
    return (lastHash - lastCharCode * pow(p, len - 1)) * p + input[len - 1];
  }

  virtual int operator()(const char *input, int len) {
    int total = 0;
    for (int i = 0; i < len; i++) {
      total += input[i] * pow(p, len - i - 1);
    }
    return total;
  }

private:
  int p;
};
