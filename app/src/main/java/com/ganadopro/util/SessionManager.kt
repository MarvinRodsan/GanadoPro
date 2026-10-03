package com.ganadopro.util

import android.content.Context

/**
 * Sesión local simple, sin autenticación remota (el login sigue siendo el
 * usuario/contraseña fijo de LoginActivity). Solo recuerda si el usuario ya
 * inició sesión una vez, para no mostrar el login en cada apertura de la app
 * ni al tocar una notificación mientras la sesión siga activa.
 */
object SessionManager {

    private const val PREFS_NAME = "ganadopro_session"
    private const val KEY_LOGGED_IN = "is_logged_in"

    fun isLoggedIn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOGGED_IN, false)
    }

    fun login(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, true).apply()
    }

    fun logout(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
