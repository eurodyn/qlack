/*
* Copyright 2014 EUROPEAN DYNAMICS SA <info@eurodyn.com>
*
* Licensed under the EUPL, Version 1.1 only (the "License").
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
* https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the Licence is distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and
* limitations under the Licence.
*/
package com.eurodyn.qlack.fuse.fileupload.response;

public class QFFIFileDeleteResponse {
	private long deletedChunks;

	public long getDeletedChunks() {
		return deletedChunks;
	}

	public void setDeletedChunks(long deletedChunks) {
		this.deletedChunks = deletedChunks;
	}

	public QFFIFileDeleteResponse(long deletedChunks) {
		super();
		this.deletedChunks = deletedChunks;
	}

}
