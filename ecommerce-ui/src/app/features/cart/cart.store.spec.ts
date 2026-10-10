import { TestBed } from '@angular/core/testing';
import { CartStore } from './cart.store';

describe('CartStore', () => {
  const keyboard = { sku: 'KB-001', name: 'Keyboard', description: '', price: 89, currency: 'USD', available: 5 };

  beforeEach(() => localStorage.clear());

  it('merges repeated adds and computes totals', () => {
    const cart = TestBed.inject(CartStore);
    cart.add(keyboard);
    cart.add(keyboard, 2);
    expect(cart.lines()).toHaveLength(1);
    expect(cart.count()).toBe(3);
    expect(cart.total()).toBe(267);
  });

  it('removes a line when quantity drops to zero', () => {
    const cart = TestBed.inject(CartStore);
    cart.add(keyboard);
    cart.setQuantity('KB-001', 0);
    expect(cart.lines()).toEqual([]);
  });
});
