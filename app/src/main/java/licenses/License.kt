package de.binarynoise.appdate

import org.intellij.lang.annotations.Language

class License(val licenseText: String) : Comparable<License> {
	
	override fun compareTo(other: License): Int = if (super.equals(other)) 0 else licenseText.compareTo(other.licenseText)
	
	override fun equals(other: Any?): Boolean = other !is Library && compareTo(other as License) == 0
	
	@Suppress("unused")
	companion object {
		
		@Language("TEXT")
		val APACHE2: License = License(
			"Licensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\n    http://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, \nsoftware distributed under the License is distributed on an \"AS IS\" BASIS, \nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. \nSee the License for the specific language governing permissions and limitations under the License. "
		)
		
		@Language("TEXT")
		val MIT: License = License(
			"The MIT License (MIT)\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software \nand associated documentation files (the \"Software\"), to deal in the Software without restriction, \nincluding without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, \nand/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, \nsubject to the following conditions:\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, \nINCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. \nIN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, \nWHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE \nOR THE USE OR OTHER DEALINGS IN THE SOFTWARE."
		)
		
		@Language("TEXT")
		val GPL3: License = License(
			"This program is free software: you can redistribute it and/or modify it \nunder the terms of the GNU General Public License as published by the Free Software Foundation, \neither version 3 of the License, or (at your option) any later version.\n" + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n" + "You should have received a copy of the GNU General Public License along with this program. If not, see <a href=\"https://www.gnu.org/licenses/#GPL\">https://www.gnu.org/licenses/#GPL</a>"
		)
		
		@Language("TEXT")
		val ISC: License = License(
			"Permission to use, copy, modify, and/or distribute this software for any purpose\nwith or without fee is hereby granted, provided that the above copyright notice \nand this permission notice appear in all copies.\n" + "THE SOFTWARE IS PROVIDED \"AS IS\" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE."
		)
		
		@Language("TEXT")
		val NTP: License = License(
			"NTP License (NTP)\n" + "Permission to use, copy, modify, and distribute this software and its documentation for any purpose with or without fee is hereby granted, provided that the above copyright notice appears in all copies and that both the copyright notice and this permission notice appear in supporting documentation, and that the name (TrademarkedName) not be used in advertising or publicity pertaining to distribution of the software without specific, written prior permission. (TrademarkedName) makes no representations about the suitability this software for any purpose. It is provided \"as is\" without express or implied warranty."
		)
	}
}
