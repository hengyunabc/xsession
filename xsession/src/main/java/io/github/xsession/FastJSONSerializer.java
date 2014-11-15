package io.github.xsession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class FastJSONSerializer implements Serializer{

	@Override
	public byte[] toData(Object object) {
		//必须要写入类名，否则反序列化得不到正确的类型
		return JSON.toJSONBytes(object, SerializerFeature.WriteClassName);
	}

	@Override
	public Object fromData(byte[] data) {
		//TODO 这里有一些类型在反序列化之后，得不到正确的类型。比如UUID
		return JSON.parse(data);
	}

}
