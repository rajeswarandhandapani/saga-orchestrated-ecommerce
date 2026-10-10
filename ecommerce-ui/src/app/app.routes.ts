import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth/guards';

export const routes: Routes = [
  { path: '', title: 'Shop', loadComponent: () => import('./features/catalog/catalog-page').then((m) => m.CatalogPage) },
  { path: 'cart', title: 'Cart', loadComponent: () => import('./features/cart/cart-page').then((m) => m.CartPage) },
  {
    path: 'orders',
    title: 'My orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders-page').then((m) => m.OrdersPage),
  },
  {
    path: 'orders/:id',
    title: 'Order',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-detail-page').then((m) => m.OrderDetailPage),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'sagas' },
      { path: 'sagas', title: 'Saga monitor', loadComponent: () => import('./features/admin/saga-monitor-page').then((m) => m.SagaMonitorPage) },
      { path: 'stock', title: 'Stock', loadComponent: () => import('./features/admin/stock-page').then((m) => m.StockPage) },
      {
        path: 'orders',
        title: 'All orders',
        data: { scope: 'all' },
        loadComponent: () => import('./features/orders/orders-page').then((m) => m.OrdersPage),
      },
    ],
  },
  { path: 'auth/callback', loadComponent: () => import('./core/auth/auth-callback').then((m) => m.AuthCallback) },
  { path: '**', redirectTo: '' },
];
