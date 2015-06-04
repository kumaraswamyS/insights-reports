package org.gooru.insights.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.gooru.insights.builders.utils.DateTime;
import org.gooru.insights.builders.utils.InsightsLogger;
import org.gooru.insights.constants.APIConstants;
import org.gooru.insights.constants.CassandraConstants.CassandraRowKeys;
import org.gooru.insights.constants.ErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CSVFileWriterServiceImpl implements CSVFileWriterService{
	
	@Autowired
	private BaseConnectionService baseConnectionService;
	
	private static final Logger logger = LoggerFactory.getLogger(CSVFileWriterServiceImpl.class);
	
	public BaseConnectionService getBaseConnectionService() {
		return baseConnectionService;
	}
	
	public void generateCSVReport(String traceId, List<String> headerKeys, List<Map<String, Object>> rowList, String fileAbsolutePath, String delimiter, Boolean isNewFile) throws FileNotFoundException {

		if(StringUtils.isBlank(delimiter)) {
			delimiter = APIConstants.PIPE;
		}
		PrintStream stream = null;
		try {
			stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(fileAbsolutePath), true)));
			
			if (isNewFile) {
				// print header row
				Iterator<String> itr = headerKeys.iterator();
				String header = null;
				while(itr.hasNext()) {
					header = itr.next();
					header = getBaseConnectionService().getColumnListFromCache(CassandraRowKeys.EXPORT_FIELDS.CassandraRowKey()).getStringValue(header, header);
					stream.print(header);
					if(itr.hasNext()) {
						stream.print(delimiter);
					}
				}
				// print new line
				stream.println(APIConstants.EMPTY);
			}
			
			StringBuilder rowLine = new StringBuilder(); 
			for (Map<String, Object> row : rowList) {
				for(String headerKey : headerKeys) {
					Object key = row.get(headerKey) == null || row.get(headerKey).equals("") || row.get(headerKey).equals(" ") ? APIConstants.NOT_APPLICABLE : row.get(headerKey);
					if(headerKey.matches(APIConstants.FIELDS_TO_TIME_FORMAT) && !key.equals(APIConstants.NOT_APPLICABLE)) {
						key = DateTime.convertMillisecondsToTime(((Number)key).longValue());
					}
					rowLine = (rowLine.length() == 0 ? rowLine.append(key) : rowLine.append(delimiter).append(key));
				}
				stream.print(rowLine);
				rowLine.setLength(APIConstants.ZERO);
				
				// print new line
				stream.println(APIConstants.EMPTY);
				stream.flush();
			}
		} catch(Exception e) {
			InsightsLogger.error(traceId, ErrorConstants.EXCEPTION_IN.replace(ErrorConstants.REPLACER,ErrorConstants.CSV_WRITER_EXCEPTION),e);
		} finally {
			stream.close();
		}
	}
	
	public void removeExpiredFile() {
		File parentDir = new File(getBaseConnectionService().getRealRepoPath());
		Date date = new Date();
		
		for(File file : parentDir.listFiles()) {
			try {
				if(file.isFile()) {
					long diffInMilliSec = date.getTime() - file.lastModified();
					long diffInHours = (diffInMilliSec / (60 * 60 * 1000));
					if(diffInHours > 24){
						file.delete();
					}
				}
			}
			catch(Exception e) {
				logger.error(ErrorConstants.REMOVING_EXPIRED_FILE, e);
			}
		}
	}
}