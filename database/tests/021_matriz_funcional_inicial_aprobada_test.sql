-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 021: inicializacion del primer GlobalAdmin
-- Dependencia: 001, 002, 003, 003.1, 003.2, 004, 005, 005.1, 006, 007,
--              008+008.1, 009, 010, 011, 013, 014+014.1, 015, 016, 017
--              VIGENTES. La semilla 021 ya debe estar aplicada; la prueba
--              valida idempotencia, formato UUID del sub y unicidad de
--              la asignacion GlobalAdmin.
-- Proposito : (a) verificar que la asignacion GlobalAdmin es unica;
--             (b) verificar que el formato UUID del sub de OGTI es
--             valido; (c) verificar que una segunda ejecucion de la
--             semilla aborta con ORA-20304; (d) verificar la presencia
--             del marcador tecnico de ejecucion unica.
-- Limpieza  : ROLLBACK TO revierte DML; los DML de la propia semilla
--             ya se ejecutaron en una transaccion previa.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T021_GLOBAL_ADMIN;

DECLARE
    v_sub_keycloak         VARCHAR2(36) := '&&SUB_KEYCLOAK';
    v_total_asignaciones   PLS_INTEGER;
    v_total_combinaciones  PLS_INTEGER;
    v_uuid_valido          PLS_INTEGER;
    v_marcador             PLS_INTEGER;
BEGIN
    -- 1) Verificar unicidad de la asignacion GlobalAdmin
    SELECT COUNT(*) INTO v_total_asignaciones
      FROM USUARIO_ROL_UNIDAD uru
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GLOBAL_ADMIN'
       AND uru.REVOCADA_EN IS NULL
       AND uru.FECHA_FIN IS NULL;
    IF v_total_asignaciones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20921,
            'T021 fallo: asignacion GlobalAdmin no es exactamente 1; conteo='
            || TO_CHAR(v_total_asignaciones));
    END IF;

    -- 2) Verificar unicidad de la combinacion ADMINISTRADOR_PIIP
    SELECT COUNT(*) INTO v_total_combinaciones
      FROM MATRIZ_FUNCION_PERFIL_UNIDAD mfu
      JOIN MATRIZ_FUNCION mf ON mf.ID_FUNCION = mfu.ID_FUNCION
     WHERE mf.CODIGO = 'ADMINISTRADOR_PIIP';
    IF v_total_combinaciones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20921,
            'T021 fallo: combinacion ADMINISTRADOR_PIIP no es exactamente 1; conteo='
            || TO_CHAR(v_total_combinaciones));
    END IF;

    -- 3) Verificar formato UUID del sub
    SELECT COUNT(*) INTO v_uuid_valido
      FROM DUAL
     WHERE REGEXP_LIKE(v_sub_keycloak,
            '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$');
    IF v_uuid_valido <> 1 THEN
        RAISE_APPLICATION_ERROR(-20921,
            'T021 fallo: el sub no tiene formato UUID valido; reasignar &&SUB_KEYCLOAK antes de reejecutar la prueba');
    END IF;

    -- 4) Verificar marcador tecnico de ejecucion unica
    SELECT COUNT(*) INTO v_marcador
      FROM USER_TAB_COMMENTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCIONAL_VERSION'
       AND COMMENTS LIKE 'PIIP 021:%';
    IF v_marcador <> 1 THEN
        RAISE_APPLICATION_ERROR(-20921,
            'T021 fallo: marcador de ejecucion unica ausente o duplicado');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'T021 OK: asignacion GlobalAdmin unica, combinacion ADMINISTRADOR_PIIP unica, sub UUID valido, marcador de ejecucion unica presente.');
END;
/

-- 5) Verificar idempotencia negativa: la segunda ejecucion debe abortar
--    con ORA-20304. Este sub-bloque se ejecuta en una subtransaccion
--    para no contaminar el SAVEPOINT principal.
SAVEPOINT T021_REINTENTO;
DECLARE
    v_sqlcode NUMBER;
BEGIN
    @@database/seeds/021_matriz_funcional_inicial_aprobada.sql
    -- Si la segunda ejecucion no aborta, la prueba falla.
    RAISE_APPLICATION_ERROR(-20921,
        'T021 fallo: la segunda ejecucion no aborto; la semilla permitio reejecucion');
EXCEPTION
    WHEN OTHERS THEN
        v_sqlcode := SQLCODE;
        IF v_sqlcode = -20304 THEN
            DBMS_OUTPUT.PUT_LINE(
                'T021 OK: la segunda ejecucion aborto con ORA-20304 como se esperaba.');
        ELSE
            RAISE;
        END IF;
END;
/
ROLLBACK TO T021_REINTENTO;

ROLLBACK TO T021_GLOBAL_ADMIN;
PROMPT T021 finalizada; filas de prueba revertidas.
