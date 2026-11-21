/**
 * Value Object cho tiền tệ VND (số nguyên, không dùng dấu phẩy động).
 */
export class Money {
  readonly vnd: number;

  private constructor(vnd: number) {
    if (!Number.isInteger(vnd)) {
      throw new Error(`Money must be an integer VND amount, got: ${vnd}`);
    }
    this.vnd = vnd;
  }

  static of(vnd: number): Money {
    return new Money(vnd);
  }

  static zero(): Money {
    return new Money(0);
  }

  plus(other: Money): Money {
    return new Money(this.vnd + other.vnd);
  }

  minus(other: Money): Money {
    return new Money(this.vnd - other.vnd);
  }

  times(multiplier: number): Money {
    if (!Number.isInteger(multiplier) || multiplier < 0) {
      throw new Error(`Invalid quantity multiplier: ${multiplier}`);
    }
    return new Money(this.vnd * multiplier);
  }

  min(other: Money): Money {
    return this.vnd <= other.vnd ? this : other;
  }

  max(other: Money): Money {
    return this.vnd >= other.vnd ? this : other;
  }

  isGreaterThanOrEqual(other: Money): boolean {
    return this.vnd >= other.vnd;
  }

  format(): string {
    return `${this.vnd.toLocaleString("vi-VN")}đ`;
  }
}
