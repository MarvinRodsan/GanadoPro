package com.ganadopro.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.ganadopro.databinding.ActivityLoginBinding
import com.ganadopro.util.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Credenciales simples (sin servidor)
    private val USUARIO = "admin"
    private val PASSWORD = "ganadopro"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya había sesión activa, no mostramos el login: pasamos directo
        // a MainActivity. Si llegamos aquí redirigidos desde una notificación
        // (MainActivity nos mandó por no tener sesión), conservamos el
        // animalId pendiente para no perder el destino.
        if (SessionManager.isLoggedIn(this)) {
            irAMain(intent.getLongExtra(MainActivity.EXTRA_ANIMAL_ID, -1L))
            return
        }

        // Permite que la app use toda la pantalla
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val usuario = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (usuario == USUARIO && password == PASSWORD) {
                SessionManager.login(this)
                irAMain(intent.getLongExtra(MainActivity.EXTRA_ANIMAL_ID, -1L))
            } else {
                Toast.makeText(
                    this,
                    "Usuario o contraseña incorrectos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun irAMain(animalIdPendiente: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (animalIdPendiente != -1L) {
                putExtra(MainActivity.EXTRA_ANIMAL_ID, animalIdPendiente)
            }
        }
        startActivity(intent)
        finish()
    }
}