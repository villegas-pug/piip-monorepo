-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 015 - cobertura US4: ciclos quincenales
-- con versionado, planificacion, presentacion del producto final, validacion
-- de resultados y cierre del proyecto
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009, 014
--              y 015 VIGENTES.
-- Proposito : verifica versionado por (ID_PROYECTO, VERSION), la UK del
--             cierre por proyecto, el CHECK CK_CP_AVANCE (0..100) y la
--             coherencia del periodo quincenal YYYY-Qn-Sn.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T015_CICLOS_US4;

DECLARE
    v_unidad   NUMBER(10) := 1;
    v_evaluador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_responsable NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_proy     NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_plan     NUMBER(12) := SEQ_PLANIFICACION_PROYECTO.NEXTVAL;
    v_plan2    NUMBER(12) := SEQ_PLANIFICACION_PROYECTO.NEXTVAL;
    v_ciclo    NUMBER(12) := SEQ_CICLO_PROYECTO.NEXTVAL;
    v_ciclo2   NUMBER(12) := SEQ_CICLO_PROYECTO.NEXTVAL;
    v_evi      NUMBER(12) := SEQ_CICLO_EVIDENCIA.NEXTVAL;
    v_parcial  NUMBER(12) := SEQ_PRODUCTO_PARCIAL.NEXTVAL;
    v_present  NUMBER(12) := SEQ_PRESENTACION_PRODUCTO_FINAL.NEXTVAL;
    v_val      NUMBER(12) := SEQ_VALIDACION_RESULTADO.NEXTVAL;
    v_cierre   NUMBER(12) := SEQ_CIERRE_PROYECTO.NEXTVAL;
    v_uk_plan  BOOLEAN := FALSE;
    v_uk_ciclo BOOLEAN := FALSE;
    v_uk_cierre BOOLEAN := FALSE;
    v_avance   BOOLEAN := FALSE;
    v_periodo  BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_evaluador, '00000000-0000-0000-0000-000000000151',
            'test015e_' || v_evaluador, 'Evaluador 015',
            'test015e_' || v_evaluador || '@example.test', 'TEST_015');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_responsable, '00000000-0000-0000-0000-000000000152',
            'test015r_' || v_responsable, 'Responsable 015',
            'test015r_' || v_responsable || '@example.test', 'TEST_015');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy, 'PRY-015-' || v_proy, 'PROYECTO', 'Proyecto 015',
            'NUEVO_SERVICIO', 'EN_EJECUCION', v_unidad, v_responsable,
            0, 'N');

    INSERT INTO PLANIFICACION_PROYECTO
        (ID_PLANIFICACION, ID_PROYECTO, ALCANCE, OBJETIVOS, VERSION, CREADO_POR)
    VALUES (v_plan, v_proy, 'Alcance 015', 'Objetivos 015', 1, 'TEST_015');

    INSERT INTO PLANIFICACION_PROYECTO
        (ID_PLANIFICACION, ID_PROYECTO, ALCANCE, OBJETIVOS, VERSION,
         ID_VERSION_ANTERIOR, CREADO_POR)
    VALUES (v_plan2, v_proy, 'Alcance 015 v2', 'Objetivos 015 v2',
            2, v_plan, 'TEST_015');

    INSERT INTO CICLO_PROYECTO
        (ID_CICLO, ID_PROYECTO, PERIODO, NUMERO_VERSION, OBJETIVOS,
         ACTIVIDADES, AVANCE, CREADO_POR)
    VALUES (v_ciclo, v_proy, '2026-Q1-S1', 1, 'Obj ciclo 1',
            'Actividades ciclo 1', 25, 'TEST_015');

    INSERT INTO CICLO_EVIDENCIA
        (ID_EVIDENCIA, ID_CICLO, ID_DOCUMENTO, CREADO_POR)
    SELECT v_evi, v_ciclo, ID_DOCUMENTO, 'TEST_015'
      FROM DOCUMENTO
     WHERE ROWNUM = 1;

    INSERT INTO PRODUCTO_PARCIAL
        (ID_PRODUCTO, ID_CICLO, DESCRIPCION, FECHA, ID_RESPONSABLE, VERSION, CREADO_POR)
    VALUES (v_parcial, v_ciclo, 'Producto parcial 015', TRUNC(SYSDATE),
            v_responsable, 1, 'TEST_015');

    INSERT INTO PRESENTACION_PRODUCTO_FINAL
        (ID_PRESENTACION, ID_PROYECTO, VERSION, DESCRIPCION, ID_RESPONSABLE, ID_DOCUMENTO_SUSTENTA)
    SELECT v_present, v_proy, 1, 'Producto final 015',
            v_responsable, ID_DOCUMENTO
      FROM DOCUMENTO
     WHERE ROWNUM = 1;

    INSERT INTO VALIDACION_RESULTADO
        (ID_VALIDACION, ID_PROYECTO, ID_RESPONSABLE, ID_EVALUADOR)
    VALUES (v_val, v_proy, v_responsable, v_evaluador);

    INSERT INTO CIERRE_PROYECTO
        (ID_CIERRE, ID_PROYECTO, INFORME_FINAL, RESULTADOS, APRENDIZAJES,
         CONCLUSION, OBSERVACION, ID_EVALUADOR)
    VALUES (v_cierre, v_proy, 'Informe 015', 'Resultados 015',
            'Aprendizajes 015', 'Conclusion 015', 'Observacion 015',
            v_evaluador);

    BEGIN
        INSERT INTO PLANIFICACION_PROYECTO
            (ID_PLANIFICACION, ID_PROYECTO, ALCANCE, OBJETIVOS, VERSION, CREADO_POR)
        VALUES (SEQ_PLANIFICACION_PROYECTO.NEXTVAL, v_proy,
                'Duplicada', 'Duplicada', 1, 'TEST_015');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_plan := TRUE;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO
            (ID_CICLO, ID_PROYECTO, PERIODO, NUMERO_VERSION, OBJETIVOS, CREADO_POR)
        VALUES (v_ciclo2, v_proy, '2026-Q1-S1', 1, 'Duplicado', 'TEST_015');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_ciclo := TRUE;
    END;

    BEGIN
        INSERT INTO CIERRE_PROYECTO
            (ID_CIERRE, ID_PROYECTO, INFORME_FINAL, RESULTADOS, APRENDIZAJES,
             CONCLUSION, OBSERVACION, ID_EVALUADOR)
        VALUES (SEQ_CIERRE_PROYECTO.NEXTVAL, v_proy,
                'Cierre duplicado', 'Cierre duplicado', 'Cierre duplicado',
                'Cierre duplicado', 'Cierre duplicado', v_evaluador);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_cierre := TRUE;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO
            (ID_CICLO, ID_PROYECTO, PERIODO, NUMERO_VERSION, OBJETIVOS,
             ACTIVIDADES, AVANCE, CREADO_POR)
        VALUES (SEQ_CICLO_PROYECTO.NEXTVAL, v_proy, '2026-Q1-S1', 2,
                'Avance fuera de rango', 'Actividades', 150, 'TEST_015');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_avance := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO
            (ID_CICLO, ID_PROYECTO, PERIODO, NUMERO_VERSION, OBJETIVOS, CREADO_POR)
        VALUES (SEQ_CICLO_PROYECTO.NEXTVAL, v_proy, '2026-Q5-S1', 1,
                'Periodo invalido', 'TEST_015');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_periodo := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk_plan OR NOT v_uk_ciclo OR NOT v_uk_cierre
       OR NOT v_avance OR NOT v_periodo THEN
        RAISE_APPLICATION_ERROR(-209F0,
            'T015 fallo: UK planificacion, UK ciclo, UK cierre, AVANCE o periodo invalido');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T015 US4 OK: planificacion, ciclos, presentacion, validacion y cierre validados.');
END;
/

ROLLBACK TO T015_CICLOS_US4;
PROMPT T015 US4 finalizada; filas de prueba revertidas.
