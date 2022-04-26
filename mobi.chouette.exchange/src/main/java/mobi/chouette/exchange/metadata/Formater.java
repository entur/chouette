package mobi.chouette.exchange.metadata;



import java.time.temporal.TemporalAccessor;

public interface Formater
{
String format(Metadata.Period period);
String format(Metadata.Box box);
String format(Metadata.Resource resource);
String formatDate(TemporalAccessor date);
}
