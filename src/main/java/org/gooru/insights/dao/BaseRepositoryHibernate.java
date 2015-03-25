package org.gooru.insights.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BaseRepositoryHibernate implements BaseRepository {
	@Autowired
	public SessionFactory sessionFactory;

	@Override
	public Object[] getAnswerByQuestionId(String questionId) {
		String sql = "SELECT c.gooru_oid AS answer_gooru_oid, answer_text FROM content c INNER JOIN assessment_answer a ON a.question_id = c.content_id WHERE is_correct = 1 AND c.gooru_oid ='" + questionId + "' LIMIT 1";
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		return (Object[]) (query.list().size() > 0 ? query.list().get(0) : null);
	}
	
	@Override
	public List<Map<String, String>> getAnswerDataList(String questionId) {
		String sql = "SELECT c.gooru_oid AS answer_gooru_oid, answer_text FROM content c INNER JOIN assessment_answer a ON a.question_id = c.content_id WHERE is_correct = 1 AND c.gooru_oid ='" + questionId + "' ";
		SQLQuery result = sessionFactory.getCurrentSession().createSQLQuery(sql);
		List<Map<String, String>> answerList = new ArrayList<Map<String, String>>();
		Map<String, String> value = new HashMap<String, String>();
		List<Object[]> results = result.list();
		if (results.size() > 0) {
			for (Object[] object : results) {
				value.put("answer_gooru_oid", object[0].toString());
				value.put("answer_text", object[1].toString());
				answerList.add(value);
			}
		}
		return answerList;
	}
	
	@Override
	public List<Object[]> getAnswerByAnswerId(String questionId) {
		String sql = "SELECT c.gooru_oid AS answer_gooru_oid, answer_text FROM content c INNER JOIN assessment_answer a ON a.answer_id = c.content_id WHERE is_correct = 1 AND c.gooru_oid ='" + questionId + "' limit 1";
		return sessionFactory.getCurrentSession().createSQLQuery(sql).list();
	}
	
	@Override
	public String getHintText(long hintId) {
		String sql = "SELECT hint_text FROM assessment_hint WHERE hint_id =" + hintId;
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		return query.list().size() > 0 ? query.list().get(0).toString() : null;
	}
	
}
