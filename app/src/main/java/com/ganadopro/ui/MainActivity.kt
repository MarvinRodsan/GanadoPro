package com.ganadopro.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ganadopro.R
import com.ganadopro.databinding.ActivityMainBinding
import com.ganadopro.repository.GanaderaRepository
import com.ganadopro.util.NotificationHelper
import com.ganadopro.util.SessionManager
import com.ganadopro.worker.AlertaWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        // Clave del extra que transporta el id del animal cuando se llega
        // acá desde una notificación (vacuna o parto próximo).
        const val EXTRA_ANIMAL_ID = "extra_animal_id"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // No bloqueamos el flujo si el usuario niega el permiso: el Worker sigue
    // corriendo igual, solo no podrá mostrar la notificación push (ver
    // NotificationHelper.mostrar). El Dashboard sigue siendo el respaldo.
    private val solicitarPermisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultado no crítico, ver comentario arriba */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Caso borde: alguien llega a MainActivity sin sesión activa (por
        // ejemplo, un PendingIntent de notificación creado antes de cerrar
        // sesión, o de un proceso que sobrevivió al logout). Lo mandamos a
        // Login sin perder el animalId pendiente.
        if (!SessionManager.isLoggedIn(this)) {
            redirigirALogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.animalesFragment,
                R.id.ventasFragment,
                R.id.reportesFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNavigation.setupWithNavController(navController)

        NotificationHelper.crearCanales(this)
        pedirPermisoNotificacionesSiHaceFalta()
        programarAlertaDiaria()

        // Caso "app cerrada": este es el intent que lanzó la Activity recién.
        manejarIntentDeepLink(intent)
    }

    // Caso "app abierta" o "app en segundo plano" con el proceso vivo:
    // gracias a launchMode="singleTop" (AndroidManifest), tocar otra
    // notificación reutiliza esta misma instancia en vez de crear una nueva.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentDeepLink(intent)
    }

    private fun manejarIntentDeepLink(intent: Intent) {
        val animalId = intent.getLongExtra(EXTRA_ANIMAL_ID, -1L)
        if (animalId == -1L) return

        // Limpiamos el extra de inmediato para no re-navegar si la Activity
        // se recrea (ej. rotación de pantalla) sin que haya llegado un
        // intent nuevo.
        intent.removeExtra(EXTRA_ANIMAL_ID)

        lifecycleScope.launch {
            val repository = GanaderaRepository(applicationContext)
            val animal = repository.getAnimalById(animalId)
            if (animal != null) {
                navController.navigate(R.id.detalleAnimalFragment, bundleOf("animalId" to animalId))
            } else {
                // El animal fue eliminado entre que se generó la alerta y se
                // tocó la notificación: no crasheamos, avisamos y nos
                // quedamos en el Dashboard.
                Toast.makeText(
                    this@MainActivity,
                    "Ese animal ya no existe en el registro",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun redirigirALogin() {
        val animalId = intent.getLongExtra(EXTRA_ANIMAL_ID, -1L)
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            if (animalId != -1L) putExtra(EXTRA_ANIMAL_ID, animalId)
        }
        startActivity(loginIntent)
        finish()
    }

    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val yaConcedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!yaConcedido) {
            solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun programarAlertaDiaria() {
        val request = PeriodicWorkRequestBuilder<AlertaWorker>(1, TimeUnit.DAYS).build()

        // enqueueUniquePeriodicWork + KEEP: si ya había un trabajo programado
        // de una sesión anterior, no se reprograma ni se duplica al volver a
        // abrir la app.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            AlertaWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_cerrar_sesion) {
            cerrarSesion()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cerrarSesion() {
        SessionManager.logout(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Limpia todo el back stack: al presionar Back desde Login
            // después de cerrar sesión, no se puede volver a MainActivity.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
