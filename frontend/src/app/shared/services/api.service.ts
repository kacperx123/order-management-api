import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProductApiResponse {
  id: string;
  name: string;
  price: number;
  active: boolean;
  available: number;
  reserved: number;
  createdAt: string;
}

export interface CreateProductRequest {
  name: string;
  price: number;
  initialStock: number;
  active: boolean;
}

export interface UpdateProductRequest {
  name?: string | null;
  price?: number | null;
  active?: boolean | null;
}

export interface UpdateStockRequest {
  delta?: number | null;
  setTo?: number | null;
}

export interface InventoryApiResponse {
  id: string;
  productId: string;
  productName: string;
  available: number;
  reserved: number;
  version: number;
}

export interface OrderItemApiResponse {
  productId: string;
  productName: string;
  quantity: number;
  unitPriceAtPurchase: number;
}

export interface OrderApiResponse {
  id: string;
  customerEmail: string;
  status: string;
  items: OrderItemApiResponse[];
  createdAt: string;
}

export interface CreateOrderItemRequest {
  productId: string;
  quantity: number;
}

export interface CreateOrderRequest {
  items: CreateOrderItemRequest[];
}

export interface OutboxEventApiResponse {
  id: string;
  aggregateType: string;
  aggregateId: string;
  type: string;
  payloadJson: string;
  occurredAt: string;
  createdAt: string;
  publishedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  constructor(private readonly http: HttpClient) {}

  listProducts(active?: boolean): Observable<ProductApiResponse[]> {
    const params = active === undefined ? {} : { params: { active: String(active) } };
    return this.http.get<ProductApiResponse[]>('/products', params);
  }

  createProduct(request: CreateProductRequest): Observable<ProductApiResponse> {
    return this.http.post<ProductApiResponse>('/products', request);
  }

  updateProduct(productId: string, request: UpdateProductRequest): Observable<ProductApiResponse> {
    return this.http.patch<ProductApiResponse>(`/products/${productId}`, request);
  }

  adjustStock(productId: string, request: UpdateStockRequest): Observable<ProductApiResponse> {
    return this.http.post<ProductApiResponse>(`/products/${productId}/stock`, request);
  }
}

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  constructor(private readonly http: HttpClient) {}

  listInventory(): Observable<InventoryApiResponse[]> {
    return this.http.get<InventoryApiResponse[]>('/inventory');
  }
}

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  constructor(private readonly http: HttpClient) {}

  listOrders(): Observable<OrderApiResponse[]> {
    return this.http.get<OrderApiResponse[]>('/orders');
  }

  listMyOrders(): Observable<OrderApiResponse[]> {
    return this.http.get<OrderApiResponse[]>('/orders/my');
  }

  getOrder(orderId: string): Observable<OrderApiResponse> {
    return this.http.get<OrderApiResponse>(`/orders/${orderId}`);
  }

  createOrder(request: CreateOrderRequest): Observable<OrderApiResponse> {
    return this.http.post<OrderApiResponse>('/orders', request);
  }

  payOrder(orderId: string): Observable<OrderApiResponse> {
    return this.http.post<OrderApiResponse>(`/orders/${orderId}/pay`, {});
  }

  cancelOrder(orderId: string): Observable<OrderApiResponse> {
    return this.http.post<OrderApiResponse>(`/orders/${orderId}/cancel`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class OutboxApiService {
  constructor(private readonly http: HttpClient) {}

  listEvents(unpublished = false): Observable<OutboxEventApiResponse[]> {
    return this.http.get<OutboxEventApiResponse[]>('/outbox-events', {
      params: { unpublished: String(unpublished) }
    });
  }
}
