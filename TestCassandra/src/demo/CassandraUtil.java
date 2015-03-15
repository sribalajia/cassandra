package demo;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.StringKeyIterator;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.*;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.CounterQuery;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.*;

/**
 * Author : asribalaji
 */
public class CassandraUtil {
    public static final String KEY = "__key";
    private static final int MAXIMUM_FETCH_ROWS = 10000;
    private static final int MAXIMUM_FETCH_COLS = 10000;


    private static CassandraUtil instance = new CassandraUtil();

    public static CassandraUtil getInstance(){
        return instance;
    }

    public Map<String, String> get(String columnFamily, String rowKey) {
        ColumnFamilyTemplate<String, String> template = new ThriftColumnFamilyTemplate<String, String>(getKeySpace(), columnFamily,
                StringSerializer.get(), StringSerializer.get());
        Map<String,String> resultMap = new HashMap<String, String>();
        try {
            template.setCount(Integer.MAX_VALUE);
            ColumnFamilyResult<String, String> res = template.queryColumns(rowKey);
            if (res.getColumnNames() == null || res.getColumnNames().size() == 0) {
                //record doesn't exist
                return null;
            }
            Collection<String> columnNames = res.getColumnNames();

            for (String columnName : columnNames) {
                if (columnName.equals("id") || columnName.equals("createdAt")) {
                    Long columnValue = res.getLong(columnName);
                    resultMap.put(columnName, String.valueOf(columnValue));
                } else {
                    resultMap.put(columnName, res.getString(columnName));
                }
            }
            resultMap.put(KEY, rowKey);
        } catch (HectorException e) {
            throw e;
        }
        return resultMap;
    }


    private static List<String> getTweetIdsForUser(String user) {
        final MultigetSliceQuery<String, Composite, String> query = HFactory.createMultigetSliceQuery(getKeySpace(),
                StringSerializer.get(),
                CompositeSerializer.get(),
                StringSerializer.get());

        query.setColumnFamily("user_tweets_day");

        Date startTime = new Date(115,2,8);
        Date endTime = new Date(115,2,9);

        String key = user + "@@@" + startTime;
        query.setKeys(key);
        startTime.setHours(20);
        query.setRange(new Composite(endTime), new Composite(startTime), true, Integer.MAX_VALUE);

        final QueryResult<Rows<String,Composite,String>> queryResult = query.execute();
        final Rows<String,Composite,String> rows = queryResult.get();
        List<String> resultList = new ArrayList<String>();
        for (Row<String, Composite, String> currentRow : rows ) {
            if (currentRow.getColumnSlice().getColumns() != null && currentRow.getColumnSlice().getColumns().size() > 0) {
                Map<String, String> currentColumnMap = new HashMap<String, String>();
                for (HColumn<Composite, String> currentColumn : currentRow.getColumnSlice().getColumns()) {
                    /*
                    if (CassandraConstants.ENTITY_TIMESTAMP_COLUMNS.contains(currentColumn.getName())) {
                        ByteBuffer bb;
                        bb = ByteBuffer.wrap(currentColumn.getValue());
                        currentColumnMap.put(currentColumn.getName(), String.valueOf(bb.getLong()));
                        continue;
                    }
                    */
                    String value = new String(currentColumn.getValue());
                    currentColumnMap.put(String.valueOf(currentColumn.getName()), value);
                    resultList.add(value);
                }
            }
        }
        return resultList;
    }

    public static void insert(String columnFamily, String key, Map<String, String> valueMap) {
        ColumnFamilyTemplate<String, String> template = new ThriftColumnFamilyTemplate<String, String>(getKeySpace(), columnFamily,
                StringSerializer.get(), StringSerializer.get());
        ColumnFamilyUpdater<String, String> updater = template.createUpdater(key);
        try {
            updateUpdater(updater,valueMap);
            template.update(updater);
        } catch (HectorException e) {
            throw new HectorException(e);
        }

    }

    private static void updateUpdater(ColumnFamilyUpdater updater, Map<String, String> valueMap) {
        int ttlValue = -1;
        if (updater != null) {
            for (String key : valueMap.keySet()) {
                if (key.equals("id") || key.equals("createdAt")) {
                    updater.setLong(key, Long.parseLong(valueMap.get(key)));
                } else {
                    updater.setString(key, valueMap.get(key));
                }
            }
        }
    }


    public List<String> listKeys(String columnFamily) {
        List<String> resultList = new ArrayList<String>();
        StringKeyIterator sk = new StringKeyIterator(getKeySpace(), columnFamily);
        Iterator<String> iterator = sk.iterator();
        try {
            while (iterator.hasNext()) {
                resultList.add(iterator.next());
            }
        } catch (HectorException e) {
            throw new HectorException(e);
        }
        return resultList;
    }

    public static void insertCompositeColumns(String columnFamily,String rowKey, Composite columnName, String columnValue) {
        List<HColumn<DynamicComposite, ?>> compositeColumns = new ArrayList<HColumn<DynamicComposite, ?>>();
        try {
            Mutator<String> mutator = HFactory.createMutator(getKeySpace(), StringSerializer.get());
            HColumn<Composite, String> hColumn =  HFactory.createColumn(columnName,columnValue, CompositeSerializer.get(), StringSerializer.get());
            mutator.addInsertion(rowKey, columnFamily, hColumn);
            mutator.execute();
        } catch (HectorException e) {
            throw new HectorException(e);
        }
    }

    public Map<String, Map<String, String>> multiGet(String
                                                              columnFamily, Collection < String > keysList){
        try {
            MultigetSliceQuery<String, String, byte[]> multigetSliceQuery =
                    HFactory.createMultigetSliceQuery(getKeySpace(), StringSerializer.get(), StringSerializer.get(), BytesArraySerializer.get());
            multigetSliceQuery.setColumnFamily(columnFamily);
            multigetSliceQuery.setKeys(keysList);
            // set null range for empty byte[] on the underlying predicate
            multigetSliceQuery.setRange(null, null, false, Integer.MAX_VALUE);
            QueryResult<Rows<String, String, byte[]>> result = multigetSliceQuery.execute();
            Rows<String, String, byte[]> orderedRows = result.get();
            Map<String, Map<String, String>> resultsMap = new HashMap<String, Map<String, String>>();
            for (Row<String, String, byte[]> currentRow : orderedRows) {
                if (currentRow.getColumnSlice().getColumns() != null && currentRow.getColumnSlice().getColumns().size() > 0) {
                    Map<String, String> currentColumnMap = new HashMap<String, String>();
                    for (HColumn<String, byte[]> currentColumn : currentRow.getColumnSlice().getColumns()) {
                        String value = new String(currentColumn.getValue());
                        currentColumnMap.put(currentColumn.getName(), value);
                    }
                    resultsMap.put(currentRow.getKey(), currentColumnMap);
                }
            }
            return resultsMap;
        } catch (HectorException e) {
            throw new HectorException(e);
        }
    }
    public static Keyspace getKeySpace() {
        try {
            CassandraHostConfigurator configurator = new CassandraHostConfigurator("127.0.0.1:9160");
            Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", configurator);
            Keyspace keyspaceObj = HFactory.createKeyspace("demo", cluster);
            configurator.setMaxActive(2);
            return keyspaceObj;
        }catch (Exception e) {
            throw new HectorException(e);
        }
    }

    public void incrementCounter(String columnFamily, String key, String columnName, long count) {
        incrementOrDecrementCounter(columnFamily, key, columnName, count, true);
    }

    public void decrementCounter(String columnFamily, String key, String columnName, long count) {
        incrementOrDecrementCounter(columnFamily, key, columnName, count, false);
    }

    private void incrementOrDecrementCounter(String columnFamily, String key, String columnName, long count, boolean increment) {
        ColumnFamilyTemplate<String, Long> template = new ThriftColumnFamilyTemplate<String, Long>(getKeySpace(), columnFamily,
                StringSerializer.get(), LongSerializer.get());
        try {
            Mutator<String> mutator = template.createMutator();
            if (increment) {
                mutator.incrementCounter(key, columnFamily, columnName, count);
            } else {
                mutator.decrementCounter(key, columnFamily, columnName, count);
            }
        } catch (HectorException e) {
            throw new HectorException(e);
        }
    }

    public long getCounterValue(String columnFamily, String key, String columnName) {
        try {
            CounterQuery<String, String> query = HFactory.createCounterColumnQuery(getKeySpace(), StringSerializer.get(), StringSerializer.get());
            query.setColumnFamily(columnFamily).setKey(key).setName(columnName);
            HCounterColumn<String> counter = query.execute().get();
            return counter != null ? counter.getValue() : 0;
        } catch (HectorException e) {
            throw new HectorException(e);
        }
    }

    public List<String> searchForKeysWithSearchMap(String columnFamily, Map<String, String> criteriaMap,String startOffset, String endOffset, int rows) {
        if (rows < 0) {
            rows = 0;
        }
        try {
            IndexedSlicesQuery<String, String, String> indexedSlicesQuery = HFactory.createIndexedSlicesQuery(getKeySpace(), StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
            indexedSlicesQuery.setColumnFamily(columnFamily);
            indexedSlicesQuery.setStartKey((startOffset != null) ? startOffset : "");
            indexedSlicesQuery.setRowCount((rows == 0) ? MAXIMUM_FETCH_ROWS : rows);
            indexedSlicesQuery.setRange(startOffset, endOffset, false, MAXIMUM_FETCH_COLS);
            indexedSlicesQuery.setReturnKeysOnly();
            for (String column : criteriaMap.keySet()) {
                indexedSlicesQuery.addEqualsExpression(column, criteriaMap.get(column));
            }
            QueryResult<OrderedRows<String, String, String>> result = indexedSlicesQuery.execute();
            OrderedRows<String, String, String> orderedRows = result.get();
            List<Row<String, String, String>> list = orderedRows.getList();
            List<String> resultList = new ArrayList<String>(list.size());
            boolean isFirstKey = true;
            for (Row<String, String, String> row : list) {
                //Checks if the first rowkey and startkey is same, when start key is not null.
                //If they don't match, results are not populated in the list and hence the method returns empty list
                if (isFirstKey && (startOffset != null && !startOffset.isEmpty()) && !startOffset.equals(row.getKey())) {
                    break;
                }
                if (isFirstKey) {
                    isFirstKey = false;
                }
                String rowKey = row.getKey();
                resultList.add(rowKey);
            }
            return resultList;
        } catch (HectorException e) {
            throw new HectorException(e);
        }
    }

}
