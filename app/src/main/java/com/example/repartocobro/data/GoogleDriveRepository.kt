package com.example.repartocobro.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile

/**
 * Repositorio para subir PDFs a Google Drive.
 *
 * Flujo:
 * 1. Verificar si hay sesión de Google activa.
 * 2. Si no → lanzar intent de Sign-In (lo maneja la Activity).
 * 3. Buscar/crear carpeta "Reco_Reportes" en Drive.
 * 4. Subir el PDF a esa carpeta.
 */
class GoogleDriveRepository(private val context: Context) {

    companion object {
        private const val FOLDER_NAME = "Reco_Reportes"
        private const val APP_NAME = "Reco"
    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    // ─────────────────────────────────────────────────────────────
    //  Autenticación
    // ─────────────────────────────────────────────────────────────

    /** Retorna la cuenta activa con permiso de Drive, o null si no hay sesión. */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        // Verificar que tenga el scope de Drive
        return if (GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            account
        } else {
            null
        }
    }

    /** Retorna true si hay sesión activa con permisos de Drive. */
    fun isSignedIn(): Boolean = getSignedInAccount() != null

    /** Retorna el Intent para iniciar el flujo de Sign-In con Google. */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /** Cierra la sesión de Google. */
    fun signOut(onComplete: () -> Unit = {}) {
        googleSignInClient.signOut().addOnCompleteListener { onComplete() }
    }

    // ─────────────────────────────────────────────────────────────
    //  Google Drive API
    // ─────────────────────────────────────────────────────────────

    /**
     * Sube un PDF a Google Drive dentro de la carpeta "Reco_Reportes".
     * DEBE ejecutarse en un hilo de fondo (Dispatchers.IO).
     *
     * @param fileName Nombre del archivo PDF (ej: "resumen_Ruta_Mateo_Mayo-21-2026.pdf")
     * @param pdfBytes Contenido del PDF en bytes
     * @return Result con el ID del archivo en Drive, o error
     */
    fun uploadPdf(fileName: String, pdfBytes: ByteArray): Result<String> {
        return runCatching {
            val account = getSignedInAccount()
                ?: throw IllegalStateException("No hay sesión de Google activa")

            val driveService = buildDriveService(account)

            // 1. Buscar o crear la carpeta
            val folderId = getOrCreateFolder(driveService, FOLDER_NAME)

            // 2. Crear metadata del archivo
            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
                mimeType = "application/pdf"
            }

            // 3. Subir el archivo
            val content = ByteArrayContent("application/pdf", pdfBytes)
            val uploadedFile = driveService.files().create(fileMetadata, content)
                .setFields("id, name")
                .execute()

            uploadedFile.id
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilidades privadas
    // ─────────────────────────────────────────────────────────────

    /** Construye el servicio de Drive API con las credenciales de la cuenta. */
    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }

    /**
     * Busca una carpeta por nombre en Drive. Si no existe, la crea.
     * @return El ID de la carpeta
     */
    private fun getOrCreateFolder(driveService: Drive, folderName: String): String {
        // Buscar carpeta existente
        val query = "mimeType='application/vnd.google-apps.folder' " +
                "and name='$folderName' " +
                "and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        if (result.files.isNotEmpty()) {
            return result.files[0].id
        }

        // Crear carpeta nueva
        val folderMetadata = DriveFile().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()

        return folder.id
    }
}
