import { describe, it, expect, vi } from 'vitest';
import { payAndRefresh } from '../../../../miniapp/core/payment';
import { assertMediaLimit, publicationMediaIds } from '../../../../packages/contracts/media';
describe('payment confirmation', () => {
  const params = {
    timeStamp: '1700000000',
    nonceStr: 'nonce',
    package: 'prepay_id=real',
    signType: 'RSA',
    paySign: 'signed',
  };
  it('does not mark paid merely because WeChat returned success', async () => {
    const launch = vi.fn().mockResolvedValue(undefined);
    const order = { id: 'order-1', paymentStatus: 'UNPAID', fulfillmentStatus: 'PENDING' };
    const get = vi.fn().mockResolvedValue(order);
    const post = vi.fn().mockResolvedValue(params);
    const result = await payAndRefresh('order-1', { post, get }, launch);
    expect(launch).toHaveBeenCalledWith(params);
    expect(result.order.paymentStatus).toBe('UNPAID');
    expect(result.notice).toContain('确认');
  });
  it('checks order truth after a cancelled payment without submitting again', async () => {
    const post = vi.fn().mockResolvedValue(params);
    const get = vi.fn().mockResolvedValue({ id: 'o', paymentStatus: 'UNPAID' });
    const result = await payAndRefresh('o', { post, get }, async () => {
      throw new Error('cancel');
    });
    expect(post).toHaveBeenCalledTimes(1);
    expect(get).toHaveBeenCalledWith('/v1/commerce/orders/o');
    expect(result.notice).toContain('未完成');
  });
  it('rejects incomplete payment parameters before launching WeChat', async () => {
    const launch = vi.fn();
    await expect(
      payAndRefresh('o', { post: async () => ({}), get: async () => ({}) }, launch),
    ).rejects.toThrow('支付参数');
    expect(launch).not.toHaveBeenCalled();
  });
});
describe('unique media limits', () => {
  it('counts shared images once while including all audio', () => {
    const draft = {
      artworks: [{ mediaIds: ['a', 'b'] }, { mediaIds: ['b'] }],
      audioMediaIds: ['c'],
    };
    expect(publicationMediaIds([draft])).toEqual(['a', 'b', 'c']);
    expect(() => assertMediaLimit(publicationMediaIds([draft]))).not.toThrow();
  });
  it('rejects the 101st distinct media before uploading or saving', () => {
    expect(() => assertMediaLimit(Array.from({ length: 101 }, (_, i) => String(i)))).toThrow('100');
  });
});
