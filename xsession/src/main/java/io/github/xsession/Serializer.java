package io.github.xsession;


/**
 * <pre>
 * 这个接口的实现类必须是线程安全的。
 * 目前有两个实现类，{@link JavaSerializer}和{@link FastJSONSerializer}。
 * JavaSerializer要求必须实现{@link java.io.Serializable}接口，兼容性最好，但体积较大。
 * FastJSONSerializer则有些类可以返序列化之后，不能正确转回原来的类型，比如UUID，要注意测试。
 * </pre>
 * 
 * @author hengyunabc
 * 
 */
public interface Serializer {
	public byte[] toData(Object object);

	public Object fromData(byte[] data);
}
