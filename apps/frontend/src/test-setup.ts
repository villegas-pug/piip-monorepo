// Configuración global para pruebas unitarias de PIIP.
//
// 1. Mock de `DataTransfer` (jsdom no lo implementa). Se usa en tests de
//    carga de archivos en initiative-form, derived-project y direct-project.
// 2. Aumenta el timeout global para `loadComponent` en rutas perezosas.

import { vi } from 'vitest';

// ---------------------------------------------------------------------------
// 1. DataTransfer mock
// ---------------------------------------------------------------------------
if (typeof globalThis.DataTransfer === 'undefined') {
  Object.defineProperty(globalThis, 'DataTransfer', {
    value: class DataTransfer {
      items = new DataTransferItemList();
      files: FileList = [] as unknown as FileList;
      clearData(): void {
        this.items = new DataTransferItemList();
      }
      getData(): string {
        return '';
      }
      setData(): void {
        // no-op
      }
      setDragImage(): void {
        // no-op
      }
    },
    configurable: true,
    writable: true
  });
}

// Para que la creación de DataTransferItemList no falle:
(globalThis as any).DataTransferItemList = class DataTransferItemList {
  private _items: File[] = [];
  get length(): number {
    return this._items.length;
  }
  add(file: File): void {
    this._items.push(file);
  }
  clear(): void {
    this._items = [];
  }
  item(index: number): File {
    return this._items[index];
  }
  remove(index: number): void {
    this._items.splice(index, 1);
  }
};

// ---------------------------------------------------------------------------
// 2. Aumentar timeout global para pruebas de carga perezosa
// ---------------------------------------------------------------------------
vi.setConfig({ testTimeout: 30000 });
