package pe.gob.midagri.piip.reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.reportes.entity.ReporteSnapshotEntity;

/**
 * Repositorio del snapshot CLOB JSON canónico que
 * comparten PDF y XLSX. El hash SHA-256 actúa como
 * UK funcional (CHECK {@code UK_RS_HASH}) y se
 * aprovecha para detectar reintentos idempotentes
 * que llegan con los mismos parámetros y el mismo
 * corte.
 */
public interface ReporteSnapshotRepository
        extends JpaRepository<ReporteSnapshotEntity, Long> {

    /** Localiza un snapshot por su hash canónico. */
    java.util.Optional<ReporteSnapshotEntity> findByHashSha256(String hashSha256);
}
