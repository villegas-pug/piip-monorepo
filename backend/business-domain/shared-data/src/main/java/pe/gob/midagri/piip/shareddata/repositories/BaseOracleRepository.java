package pe.gob.midagri.piip.shareddata.repositories;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class BaseOracleRepository {

   protected final JdbcTemplate jdbcTemplate;
   protected final DataSource dataSource;

   protected void executeProcedureWithInParams(String procedureName, Map<String, Object> inParams) {
      SimpleJdbcCall jdbcCall = new SimpleJdbcCall(this.jdbcTemplate);
      jdbcCall.withProcedureName(procedureName);
      jdbcCall.execute(inParams);
   }

   protected <T> List<T> executeProcedureWithInParams(String procedureName, Map<String, Object> inParams,
         String outParamName, Class<T> resultType) {

      SimpleJdbcCall jdbcCall = new SimpleJdbcCall(this.jdbcTemplate);
      jdbcCall.withProcedureName(procedureName);
      jdbcCall.returningResultSet(outParamName, BeanPropertyRowMapper.newInstance(resultType));

      Map<String, Object> outParams = jdbcCall.execute(inParams);

      return (List<T>) outParams.get(outParamName);
   }

   public List<Map<String, Object>> executeProcedureAndFetchResult(String procedureName, Map<String, Object> inParams,
         String outParamName) {

      // ? Nombre alternativo: executeProcedureWithParams

      SimpleJdbcCall jdbcCall = new SimpleJdbcCall(this.jdbcTemplate);
      jdbcCall.withProcedureName(procedureName);
      jdbcCall.returningResultSet(outParamName, new ColumnMapRowMapper());

      Map<String, Object> outParam;

      if (inParams != null) {
         outParam = jdbcCall.execute(inParams);
      } else {
         outParam = jdbcCall.execute();
      }

      return (List<Map<String, Object>>) outParam.get(outParamName);
   }

   protected <T> T executeProcedureWithOutParam(String procedureName, Map<String, Object> inParams,
         String outParamName, Class<T> resultType) {
      SimpleJdbcCall jdbcCall = new SimpleJdbcCall(this.jdbcTemplate);
      jdbcCall.withProcedureName(procedureName);
      jdbcCall.declareParameters(new SqlOutParameter(outParamName, this.getSqlType(resultType)));
      Map<String, Object> outParams = jdbcCall.execute(inParams);
      return (T) outParams.get(outParamName);
   }

   private int getSqlType(Class<?> clazz) {
      return switch (clazz.getName()) {
         case "java.lang.String" -> Types.VARCHAR;
         case "java.lang.Integer" -> Types.INTEGER;
         default -> Types.VARCHAR;
      };
   }

}
