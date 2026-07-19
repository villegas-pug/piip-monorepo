package pe.gob.midagri.piip.shareddata.enums;

public enum InabifServices {

   CEDIF(1L, "CEDIF"),
   PUNCHE(2L, "PUNCHE"),
   ACERCANDONOS(3L, "ACERCANDONOS"),
   SIGESU(4L, "SIGESU"),
   EDUCALLE(5L, "EDUCALLE");

   private final Long id;
   private final String nombre;

   InabifServices(Long id, String nombre) {
      this.id = id;
      this.nombre = nombre;
   }

   public Long getId() {
      return this.id;
   }

   public String getNombre() {
      return this.nombre;
   }

}
