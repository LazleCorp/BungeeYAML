package net.craftminecraft.bungee.bungeeyaml.supereasyconfig;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.md_5.bungee.api.ProxyServer;
import net.craftminecraft.bungee.bungeeyaml.bukkitapi.ConfigurationSection;

/*
 * SuperEasyConfig - ConfigObject
 * 
 * Based off of codename_Bs EasyConfig v2.1
 * which was inspired by md_5
 * 
 * An even awesomer super-duper-lazy Config lib!
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * @author MrFigg
 * @version 1.2
 */

public abstract class ConfigObject {
	
	/*
	 *  loading and saving
	 */
	
	protected void onLoad(ConfigurationSection cs) throws Exception {
		for(Field field : getClass().getDeclaredFields()) {
			String path = field.getName().replaceAll("_", ".");
			if(doSkip(field)) {
				// Do nothing
			} else if(cs.isSet(path)) {
				field.set(this, loadObject(field, cs, path));
			} else {
				cs.set(path, saveObject(field.get(this), field, cs, path));
			}
		}
	}
	
	protected void onSave(ConfigurationSection cs) throws Exception {
		for(Field field : getClass().getDeclaredFields()) {
			String path = field.getName().replaceAll("_", ".");
			if(doSkip(field)) {
				// Do nothing
			} else {
				cs.set(path, saveObject(field.get(this), field, cs, path));
			}
		}
	}
	
	protected Object loadObject(Field field, ConfigurationSection cs, String path) throws Exception {
		return loadObject(field, cs, path, 0);
	}
	
	protected Object saveObject(Object obj, Field field, ConfigurationSection cs, String path) throws Exception {
		return saveObject(obj, field, cs, path, 0);
	}
	
	@SuppressWarnings("rawtypes")
	protected Object loadObject(Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		Class clazz = getClassAtDepth(field.getGenericType(), depth);
		if(ConfigObject.class.isAssignableFrom(clazz)&&isConfigurationSection(cs.get(path))) {
			return getConfigObject(clazz, cs.getConfigurationSection(path));
		} else if(Map.class.isAssignableFrom(clazz)&&isConfigurationSection(cs.get(path))) {
			return getMap(field, cs.getConfigurationSection(path), path, depth);
		} else if(clazz.isEnum()&&isString(cs.get(path))) {
			return getEnum(clazz, (String) cs.get(path));
		} else if(List.class.isAssignableFrom(clazz)&&isConfigurationSection(cs.get(path))) {
			Class subClazz = getClassAtDepth(field.getGenericType(), depth+1);
			if(ConfigObject.class.isAssignableFrom(subClazz)||Map.class.isAssignableFrom(subClazz)||List.class.isAssignableFrom(subClazz)||subClazz.isEnum()) {
				return getList(field, cs.getConfigurationSection(path), path, depth);
			} else {
				return cs.get(path);
			}
		} else {
			return cs.get(path);
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected Object saveObject(Object obj, Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		Class clazz = getClassAtDepth(field.getGenericType(), depth);
		if(ConfigObject.class.isAssignableFrom(clazz)&&isConfigObject(obj)) {
			return getConfigObject((ConfigObject) obj, path, cs);
		} else if(Map.class.isAssignableFrom(clazz)&&isMap(obj)) {
			return getMap((Map) obj, field, cs, path, depth);
		} else if(clazz.isEnum()&&isEnum(clazz, obj)) {
			return getEnum((Enum) obj);
		} else if(List.class.isAssignableFrom(clazz)&&isList(obj)) {
			Class subClazz = getClassAtDepth(field.getGenericType(), depth+1);
			if(ConfigObject.class.isAssignableFrom(subClazz)||Map.class.isAssignableFrom(subClazz)||List.class.isAssignableFrom(subClazz)||subClazz.isEnum()) {
				return getList((List) obj, field, cs, path, depth);
			} else {
				return obj;
			}
		} else {
			return obj;
		}
	}
	
	/*
	 * class detection
	 */
	
	@SuppressWarnings("rawtypes")
	protected Class getClassAtDepth(Type type, int depth) throws Exception {
		if(depth<=0) {
			String className = type.toString();
			if(className.length()>=6&&className.substring(0, 6).equalsIgnoreCase("class ")) {
				className = className.substring(6);
			}
			if(className.indexOf("<")>=0) {
				className = className.substring(0, className.indexOf("<"));
			}
			try {
				return Class.forName(className);
			} catch(ClassNotFoundException ex) {
				// ugly fix for primitive data types
				if(className.equalsIgnoreCase("byte")) return Byte.class;
				if(className.equalsIgnoreCase("short")) return Short.class;
				if(className.equalsIgnoreCase("int")) return Integer.class;
				if(className.equalsIgnoreCase("long")) return Long.class;
				if(className.equalsIgnoreCase("float")) return Float.class;
				if(className.equalsIgnoreCase("double")) return Double.class;
				if(className.equalsIgnoreCase("char")) return Character.class;
				if(className.equalsIgnoreCase("boolean")) return Boolean.class;
				throw ex;
			}
		}
		depth--;
		ParameterizedType pType = (ParameterizedType) type;
		Type[] typeArgs = pType.getActualTypeArguments();
		return getClassAtDepth(typeArgs[typeArgs.length-1], depth);
	}
	
	protected boolean isString(Object obj) {
		if(obj instanceof String) {
			return true;
		}
		return false;
	}
	
	protected boolean isConfigurationSection(Object o) {
		try {
			return (ConfigurationSection) o != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	protected boolean isConfigObject(Object obj) {
		try {
			return (ConfigObject) obj != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected boolean isMap(Object obj) {
		try {
			return (Map) obj != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected boolean isList(Object obj) {
		try {
			return (List) obj != null;
		} catch(Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected boolean isEnum(Class clazz, Object obj) {
		if(!clazz.isEnum()) return false;
		for(Object constant : clazz.getEnumConstants()) {
			if(constant.equals(obj)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * loading conversion
	 */
	
	@SuppressWarnings("rawtypes")
	protected ConfigObject getConfigObject(Class clazz, ConfigurationSection cs) throws Exception {
		ConfigObject obj = (ConfigObject) clazz.newInstance();
		obj.onLoad(cs);
		return obj;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Map getMap(Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		depth++;
		Set<String> keys = cs.getKeys(false);
		Map map = new HashMap();
		if(keys != null && keys.size() > 0) {
			for(String key : keys) {
				Object in = cs.get(key);
				in = loadObject(field, cs, key, depth);
				map.put(key, in);
			}
		}
		return map;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List getList(Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		depth++;
		int listSize = cs.getKeys(false).size();
		String key = path;
		if(key.lastIndexOf(".")>=0) {
			key = key.substring(key.lastIndexOf("."));
		}
		List list = new ArrayList();
		if(listSize > 0) {
			int loaded = 0;
			int i = 0;
			while(loaded<listSize) {
				if(cs.isSet(key+i)) {
					Object in = cs.get(key+i);
					in = loadObject(field, cs, key+i, depth);
					list.add(in);
					loaded++;
				}
				i++;
				// ugly overflow guard... should only be needed if config was manually edited very badly
				if(i>(listSize*3)) loaded = listSize;
			}
		}
		return list;
	}
	
	@SuppressWarnings("rawtypes")
	protected Enum getEnum(Class clazz, String string) throws Exception {
		if(!clazz.isEnum()) throw new Exception("Class "+clazz.getName()+" is not an enum.");
		for(Object constant : clazz.getEnumConstants()) {
			if(((Enum) constant).toString().equals(string)) {
				return (Enum) constant;
			}
		}
		throw new Exception("String "+string+" not a valid enum constant for "+clazz.getName());
	}
	
	/*
	 * saving conversion
	 */
	
	protected ConfigurationSection getConfigObject(ConfigObject obj, String path, ConfigurationSection cs) throws Exception {
		ConfigurationSection subCS = cs.createSection(path);
		obj.onSave(subCS);
		return subCS;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ConfigurationSection getMap(Map map, Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		depth++;
		ConfigurationSection subCS = cs.createSection(path);
		Set<String> keys = map.keySet();
		if(keys != null && keys.size() > 0) {
			for(String key : keys) {
				Object out = map.get(key);
				out = saveObject(out, field, cs, path+"."+key, depth);
				subCS.set(key, out);
			}
		}
		return subCS;
	}
	
	@SuppressWarnings("rawtypes")
	protected ConfigurationSection getList(List list, Field field, ConfigurationSection cs, String path, int depth) throws Exception {
		depth++;
		ConfigurationSection subCS = cs.createSection(path);
		String key = path;
		if(key.lastIndexOf(".")>=0) {
			key = key.substring(key.lastIndexOf("."));
		}
		if(list != null && list.size() > 0) {
			for(int i = 0; i < list.size(); i++) {
				Object out = list.get(i);
				out = saveObject(out, field, cs, path+"."+key+(i+1), depth);
				subCS.set(key+(i+1), out);
			}
		}
		return subCS;
	}
	
	@SuppressWarnings("rawtypes")
	protected String getEnum(Enum enumObj) {
		return enumObj.toString();
	}
	
	/*
	 * utility
	 */
	
	protected boolean doSkip(Field field) {
		return Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) || Modifier.isPrivate(field.getModifiers());
	}
}
