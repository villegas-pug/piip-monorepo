import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemViolation {
  readonly field?: string;
  readonly message: string;
}

export interface ProblemDetails {
  readonly type: string;
  readonly title: string;
  readonly status: number;
  readonly code?: string;
  readonly detail?: string;
  readonly instance?: string;
  readonly correlationId?: string;
  readonly violations: readonly ProblemViolation[];
}

/** Convierte únicamente respuestas Problem Details declaradas por el API, sin asumir formas ajenas. */
export function parseProblemDetails(error: unknown): ProblemDetails | undefined {
  if (!(error instanceof HttpErrorResponse) || !isProblemContentType(error.headers.get('Content-Type')) || !isRecord(error.error)) {
    return undefined;
  }

  const body = error.error as Record<string, unknown>;
  const status = numberValue(body['status']) ?? error.status;
  const title = textValue(body['title']);
  if (!title || !Number.isInteger(status) || status < 400 || status > 599) {
    return undefined;
  }

  return Object.freeze({
    type: textValue(body['type']) ?? 'about:blank',
    title,
    status,
    code: textValue(body['code']),
    detail: textValue(body['detail']),
    instance: textValue(body['instance']),
    correlationId: textValue(body['correlationId']),
    violations: parseViolations(body['violations'])
  });
}

function isProblemContentType(value: string | null): boolean {
  return value?.split(';', 1)[0]?.trim().toLowerCase() === 'application/problem+json';
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function textValue(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function numberValue(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

function parseViolations(value: unknown): readonly ProblemViolation[] {
  if (!Array.isArray(value)) {
    return Object.freeze([]);
  }

  return Object.freeze(
    value.flatMap((violation: unknown): ProblemViolation[] => {
      if (!isRecord(violation)) {
        return [];
      }
      const message = textValue(violation['message']);
      return message ? [{ field: textValue(violation['field']), message }] : [];
    })
  );
}
