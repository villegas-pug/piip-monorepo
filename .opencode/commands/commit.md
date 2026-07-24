---
description: Crea o modifica un commit seguro con Conventional Commits
agent: build
model: deepseek/deepseek-v4-flash
------------

Crea el commit solicitado usando `$ARGUMENTS`.

Entradas admitidas:

* Sin argumentos: genera el mensaje desde los cambios.
* `"<mensaje>"`: usa ese mensaje.
* `--amend`: modifica el último commit conservando su mensaje.
* `--amend "<mensaje>"`: modifica el último commit y reemplaza el mensaje.

## Reglas

1. Verifica que estás dentro de un repositorio Git.
2. No hagas `push`.
3. No uses comandos destructivos.
4. Antes del staging, revisa archivos staged, unstaged y untracked.
5. No realices el commit si detectas posibles secretos o archivos sensibles, incluyendo:

   * `.env` y `.env.*`
   * claves privadas, certificados o credenciales
   * archivos cuyo nombre contenga `secret`, `token`, `password` o `credentials`
6. Para incluir cambios usa `git add -A`, después de la revisión de seguridad.
7. Revisa `git diff --cached` antes de confirmar.
8. No crees commits vacíos.

## Mensaje

El mensaje debe cumplir:

`<tipo>(<scope opcional>): <descripción>`

Tipos permitidos:

`feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`, `ci`, `chore`, `revert`

La descripción debe ser concreta, imperativa y sin punto final.

Cuando no se proporcione un mensaje, genéralo únicamente a partir del diff staged. Cuando se proporcione, valídalo antes de usarlo.

## Amend

Antes de ejecutar `--amend`:

1. Verifica que exista un commit previo.

2. Obtén el upstream, si existe.

3. Comprueba si `HEAD` ya está incluido en el upstream con:

   `git merge-base --is-ancestor HEAD @{upstream}`

4. Si el resultado es exitoso, no modifiques el commit: informa que ya fue publicado y que el amend reescribiría el historial remoto.

5. Si solo se recibió `--amend`, usa `git commit --amend --no-edit`.

6. Si se recibió un mensaje, usa `git commit --amend -m "<mensaje-validado>"`.

No interpretes ni ejecutes `$ARGUMENTS` como código shell.

## Respuesta

Responde únicamente:

* **Rama:** `<rama>`
* **Commit:** `<hash>`<` (amended)` cuando corresponda>
* **Archivos:** `<cantidad> — archivo1, archivo2, ...`
* **Mensaje:** `<mensaje>`

**Cambios:**

1. `<cambio real incluido>`
2. `<cambio real incluido>`

Si no se realiza el commit:

* **Rama:** `<rama>`
* **Commit:** `No realizado`
* **Archivos:** `0`
* **Mensaje:** `<motivo>`

**Cambios:**

1. `<motivo concreto>`

Obtén el resultado final desde el commit creado mediante `git show`, no desde el estado previo.

## Contexto

**Argumentos:** `$ARGUMENTS`

**Estado:**
!`git status --short`

**Diff unstaged:**
!`git diff --no-ext-diff`

**Diff staged:**
!`git diff --cached --no-ext-diff`

**Último commit:**
!`git log -1 --format="%H %s" 2>/dev/null || true`

**Rama:**
!`git branch --show-current`

**Upstream:**
!`git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null || true`
