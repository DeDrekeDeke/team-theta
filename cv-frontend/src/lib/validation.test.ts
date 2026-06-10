import { describe, expect, it } from 'vitest';

import { required } from './validation';

describe('required', () => {
  it('accepts text that remains after trimming whitespace', () => {
    expect(required(' Candidate name ')).toBe(true);
  });

  it('rejects blank values', () => {
    expect(required('  \n\t  ')).toBe(false);
  });
});
