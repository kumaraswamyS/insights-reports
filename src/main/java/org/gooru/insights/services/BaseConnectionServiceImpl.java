package org.gooru.insights.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.NodeBuilder;
import org.gooru.insights.constants.CassandraConstants;
import org.gooru.insights.constants.CassandraConstants.keyspaces;
import org.gooru.insights.constants.ESConstants.esConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.impl.SmaLatencyScoreStrategyImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

@Service
public class BaseConnectionServiceImpl implements BaseConnectionService,CassandraConstants {

	 protected static final ConsistencyLevel LOCAL_CONSISTENCY_LEVEL = ConsistencyLevel.CL_ONE;
	 protected static final ConsistencyLevel DEFAULT_CONSISTENCY_LEVEL = ConsistencyLevel.CL_QUORUM;
	 
	private static Client client;
	
	private static Keyspace insightsKeyspace;
	
	private static Keyspace searchKeyspace;
	
	@Autowired
	BaseAPIService baseAPIService;
	
	@Resource(name="cassandra")
	Properties cassandra;
	
	public Properties getCassandraProperties() {
		return cassandra;
	}
	@PostConstruct
	public void initConnect(){
		
		if(insightsKeyspace == null || searchKeyspace == null){
			System.out.println(" init insights keyspace "+ insightsKeyspace +" and search keyspace "+searchKeyspace);
			
		initCassandraConnection();
		}
		
		if(client == null ){
			System.out.println("init es client "+ client);
			
			initESConnection();
		}
	}
	public Client getClient(){
	
		return this.client;
	}
	
	public void initCassandraConnection(){
	
		 String seeds = this.getCassandraProperties().getProperty(cassandraConfigs.SEEDS.cassandraConfig());
         Integer port = Integer.parseInt(this.getCassandraProperties().getProperty(cassandraConfigs.PORT.cassandraConfig()));
         String clusterName = this.getCassandraProperties().getProperty(cassandraConfigs.CLUSTER.cassandraConfig());
         String insights = this.getCassandraProperties().getProperty(cassandraConfigs.INSIGHTS_KEYSPACE.cassandraConfig());
         String search = this.getCassandraProperties().getProperty(cassandraConfigs.SEARCH_KEYSPACE.cassandraConfig());

		AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
        .forCluster(clusterName)
        .forKeyspace(insights)
        .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
        .setDiscoveryType(NodeDiscoveryType.NONE)
        .setConnectionPoolType(ConnectionPoolType.TOKEN_AWARE))
        .withConnectionPoolConfiguration(connectionConfig(seeds,port))
        .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
        .buildKeyspace(ThriftFamilyFactory.getInstance());
		context.start();
		insightsKeyspace = (Keyspace)context.getClient();
		
		AstyanaxContext<Keyspace> context2 = new AstyanaxContext.Builder()
        .forCluster(clusterName)
        .forKeyspace(search)
        .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
        .setDiscoveryType(NodeDiscoveryType.NONE)
        .setConnectionPoolType(ConnectionPoolType.TOKEN_AWARE))
        .withConnectionPoolConfiguration(connectionConfig(seeds,port))
        .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
        .buildKeyspace(ThriftFamilyFactory.getInstance());
		context2.start();
		searchKeyspace = context2.getClient();
		System.out.println("insights keyspace "+ insightsKeyspace +" and search keyspace "+searchKeyspace);
	}
	public ConnectionPoolConfigurationImpl connectionConfig(String seeds,Integer port){
		StringBuffer seedConfigured = new StringBuffer();
		for(String seed : seeds.split(",")){
			if(seedConfigured.length() > 0){
				seedConfigured.append(",");
			}
			seedConfigured.append(seed+":"+port);
		}
		
		System.out.println("seeds"+seedConfigured.toString());
		ConnectionPoolConfigurationImpl connectionPoolConfig = new ConnectionPoolConfigurationImpl("MyConnectionPool")
		.setPort(port.intValue())
		.setMaxConnsPerHost(30)
		.setSeeds(seedConfigured.toString());
		
		if (!seeds.startsWith("127.0")) {
			connectionPoolConfig.setLocalDatacenter("datacenter1");
		}
		
		connectionPoolConfig.setLatencyScoreStrategy(new SmaLatencyScoreStrategyImpl()); // Enabled SMA.  Omit this to use round robin with a token range
		return connectionPoolConfig;
	}
	
	public void initESConnection(){
		OperationResult<ColumnList<String>> rowResult =readColumns(keyspaces.INSIGHTS.keyspace(), columnFamilies.CONNECTION_CONFIG_SETTING.columnFamily(),esConfigs.ROWKEY.esConfig(), new ArrayList<String>());
		ColumnList<String> columnList = rowResult.getResult();
		System.out.println("column list");
		String indexName = columnList.getColumnByName(esConfigs.INDEX.esConfig()).getStringValue();
		System.out.println(" index "+columnList.getColumnByName(esConfigs.INDEX.esConfig()).getStringValue());
		String clusterName = columnList.getStringValue(esConfigs.CLUSTER.esConfig(),"") ;
		System.out.println(" cluster "+clusterName);

		String hostName = columnList.getColumnByName(esConfigs.HOSTS.esConfig()).getStringValue();
		System.out.println(" hostname "+hostName);

		String portNo = columnList.getColumnByName(esConfigs.PORTNO.esConfig()).getStringValue();
		System.out.println(" portNo "+portNo);

		String nodeType = columnList.getColumnByName(esConfigs.NODE.esConfig()).getStringValue();
		System.out.println(" nodeType "+nodeType);

//	String indexName ="event_logger";
//	String clusterName="";
//	String hostName="162.243.130.94";
//	String portNo="9300";
//	String nodeType ="transportClient";
		System.out.println("nodeType "+nodeType);
		if(nodeType != null && !nodeType.isEmpty()){
		if(esConfigs.NODE_CLIENT.esConfig().equalsIgnoreCase(nodeType)){
			client  = initNodeClient(clusterName);
		}
		}
		if(client == null){
			client = initTransportClient(hostName,portNo,clusterName,indexName);
		}
	}
	
	public Client initNodeClient(String clusterName){
		 Settings settings = ImmutableSettings.settingsBuilder().put(esConfigs.ES_CLUSTER.esConfig(), clusterName != null ? clusterName : "").put("client.transport.sniff", true).build();
		 return new NodeBuilder().settings(settings).node().client();   	
	}
	
	public Client initTransportClient(String hostName,String portNo,String clusterName,String indexName){
		 Settings settings = ImmutableSettings.settingsBuilder().put(esConfigs.ES_CLUSTER.esConfig(), clusterName != null ? clusterName : "").put("client.transport.sniff", true).build();
         TransportClient transportClient = new TransportClient(settings);
         transportClient.addTransportAddress(new InetSocketTransportAddress(hostName, Integer.valueOf(portNo)));
         return transportClient;
	}
	
	public Keyspace connectInsights(){
		return insightsKeyspace;
	}
	
	public Keyspace connectSearch(){
		return searchKeyspace;
	}

	public OperationResult<ColumnList<String>> readColumns(String keyspace, String columnFamilyName, String rowKey,Collection<String> columns) {

		Keyspace queryKeyspace = null;
		if (keyspaces.INSIGHTS.keyspace().equalsIgnoreCase(keyspace)) {
			queryKeyspace = insightsKeyspace;
		} else {
			queryKeyspace = searchKeyspace;
		}
		try {
			System.out.println("keyspace "+queryKeyspace);
			RowQuery<String, String> rowQuery = queryKeyspace.prepareQuery(this.accessColumnFamily(columnFamilyName)).setConsistencyLevel(LOCAL_CONSISTENCY_LEVEL).getKey(rowKey);
		
			if(baseAPIService.checkNull(columns)){
				rowQuery.withColumnSlice(columns);
			}
			return rowQuery.execute();
		} catch (ConnectionException e) {

			e.printStackTrace();
			System.out.println("Query execution exeption");
		}
		return null;

	}
	
	public ColumnFamily<String, String> accessColumnFamily(String columnFamilyName) {

		ColumnFamily<String, String> columnFamily;

		columnFamily = new ColumnFamily<String, String>(columnFamilyName, StringSerializer.get(), StringSerializer.get());

		return columnFamily;
	}
}

