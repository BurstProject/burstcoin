package brs.db.converter;

import java.math.BigInteger;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class BigIntegerConverter implements AttributeConverter<BigInteger, byte[]> {
  @Override
  public byte[] convertToDatabaseColumn(BigInteger v) {
    return v.toByteArray();
  }

  @Override
  public BigInteger convertToEntityAttribute(byte[] v) {
    return new BigInteger(v);
  }
}
