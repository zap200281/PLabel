package com.pcl.pojo;

import java.io.Serializable;

public class FileResult implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6195010905900695962L;

	private String code;
	
	private String code_msg;
	
	private String etag;
	
	private String public_url;
	
	private String presigned_url;
	
	private String object_name;
	
	private String bucket_name;
	
	private String origin_file_name;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCode_msg() {
		return code_msg;
	}

	public void setCode_msg(String code_msg) {
		this.code_msg = code_msg;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getPublic_url() {
		return public_url;
	}

	public void setPublic_url(String public_url) {
		this.public_url = public_url;
	}

	public String getPresigned_url() {
		return presigned_url;
	}

	public void setPresigned_url(String presigned_url) {
		this.presigned_url = presigned_url;
	}

	public String getObject_name() {
		return object_name;
	}

	public void setObject_name(String object_name) {
		this.object_name = object_name;
	}

	public String getBucket_name() {
		return bucket_name;
	}

	public void setBucket_name(String bucket_name) {
		this.bucket_name = bucket_name;
	}

	public String getOrigin_file_name() {
		return origin_file_name;
	}

	public void setOrigin_file_name(String origin_file_name) {
		this.origin_file_name = origin_file_name;
	}
	
	
}
