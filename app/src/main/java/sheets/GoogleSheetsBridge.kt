package de.binarynoise.appdate

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.*

object GoogleSheetsBridge {
	private val applicationName = getString(R.string.app_name)
	private const val spreadsheetId = "1cq4gZLevu0hPIcaP6Z4Hp46hrmH47R7Zd83XSNNKp3g"
	private const val range = "templates!A2:C1000"
	private val httpTransport: HttpTransport
	private val sheets: Sheets
	private val credentials: HttpCredentialsAdapter
	
	fun getValues(): List<List<Any>> {
		val values = sheets.spreadsheets().values().get(spreadsheetId, range).execute().getValues()
		return values ?: emptyList()
	}
	
	init {
		try {
			val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
			globalContext.assets.open("google.bks").use { keyStore.load(it, null) }
			httpTransport = NetHttpTransport.Builder().trustCertificates(keyStore).build()
			
			
			credentials = authExplicit()
			
			sheets = Sheets.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credentials)
				.setApplicationName(applicationName).build()
		} catch (e: GeneralSecurityException) {
			throw RuntimeException(e.message, e)
		} catch (e: IOException) {
			throw RuntimeException(e.message, e)
		}
	}
	
	@RunInBackground
	@Throws(IOException::class)
	fun updateValues(newValues: List<List<Any>>) {
		val valueRange = ValueRange()
		valueRange.setValues(newValues)
		sheets.spreadsheets().values().update(spreadsheetId, range, valueRange).setValueInputOption("raw").execute()
	}
	
	@Throws(IOException::class)
	private fun authExplicit(): HttpCredentialsAdapter {
		val scopes = ArrayList<String>()
		scopes.add("https://www.googleapis.com/auth/cloud-platform")
		scopes.addAll(SheetsScopes.all())
		return HttpCredentialsAdapter(
				GoogleCredentials.fromStream(ByteArrayInputStream(Tokens.googleCred.toByteArray())).createScoped(scopes))
	}
}
