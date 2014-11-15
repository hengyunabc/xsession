package io.github.xsession.test;

import io.github.xsession.FastJSONSerializer;
import io.github.xsession.JavaSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class FastJSONSerializerTest {
	
	@Test
	public void test() {
		FastJSONSerializer  serializer = new FastJSONSerializer();
		
		JavaSerializer javaSerializer = new JavaSerializer();
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("hello", "xxxx");
		map.put("abc", UUID.randomUUID());
		map.put("student", new Student());
		
		byte[] data = serializer.toData(map);
		System.err.println(data.length);
		
		System.err.println(new String(data));
		
		Object fromData = serializer.fromData(data);
		
		System.err.println(fromData.getClass().getName());
		
		Map<String, Object> map2 = (Map<String, Object>) fromData;
		Object object = map2.get("abc");
		
		System.err.println(object.getClass().getName());
		
		System.err.println(map2.get("student").getClass().getName());
		
		byte[] data2 = javaSerializer.toData(map);
		System.err.println(data2.length);
	}
}
