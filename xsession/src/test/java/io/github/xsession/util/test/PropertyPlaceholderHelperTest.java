package io.github.xsession.util.test;

import io.github.xsession.util.PropertyPlaceholderHelper;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class PropertyPlaceholderHelperTest {

	@Test
	public void test() {
		
		PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");
		
		helper = new PropertyPlaceholderHelper("${", "}", ":", true);
		
		Properties properties = new Properties();
		properties.put("name", "xxxxx");
		
		String value = "slfjsdlkf${name:defaultName}name";
		
		String address = "slfjsdlkf${address:defaultAddress}aaaa";
		
		
		String placeholders = helper.replacePlaceholders(value, properties);
		
		System.err.println(placeholders);
		
		Assert.assertEquals(placeholders, "slfjsdlkfxxxxxname");
		
		
		String placeholders2 = helper.replacePlaceholders(address, properties);
		
		System.err.println(placeholders2);
		
		Assert.assertEquals(placeholders2, "slfjsdlkfdefaultAddressaaaa");
	}
}
