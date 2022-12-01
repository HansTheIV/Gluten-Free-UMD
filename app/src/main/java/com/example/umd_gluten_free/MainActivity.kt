package com.example.umd_gluten_free

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.umd_gluten_free.ui.theme.UMDGlutenFreeTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.security.MessageDigest


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContent {
            UMDGlutenFreeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    AppNavHost(auth = auth, context = LocalContext.current, listenerOwner = this)


                }
            }
        }
    }

}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "mapScreen",
    auth: FirebaseAuth,
    context: Context,
    listenerOwner: MainActivity
) {
    NavHost(modifier=modifier, navController=navController, startDestination=startDestination) {
        composable("accountManagement") {
            if (auth.currentUser != null) {
                // If the user is currently logged in
                LogoutScreen(auth = auth, onNavigateToMap = {navController.navigate("mapScreen")})
            } else {
                LoginScreen(
                    onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                    onNavigateToSignup = {navController.navigate("signupScreen")},
                    auth = auth,
                    context = context
                )
            }
        }
        composable("listScreen") { ListScreen() }
        composable("submitNewFood") { SubmitScreen(auth = auth, context = context) {
            navController.navigate(
                "loginScreen"
            )
        }
        }
        composable("forgotPassword") {ForgotPasswordScreen(auth = auth, context = context)}
        composable("signupScreen") {SignupScreen(auth = auth, context = context, listenerOwner = listenerOwner)}
        composable("loginScreen") {
            LoginScreen(
                onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                onNavigateToSignup = {navController.navigate("signupScreen")},
                auth = auth,
                context = context
            )
        }
        composable("mapScreen") {
            MapScreen(
                onNavigateToSettings = {
                    navController.navigate("accountManagement") {
                        popUpTo("mapScreen")
                    }
                },
                onNavigateToList = {
                    navController.navigate("listScreen")
                },
                onNavigateToSubmit = {
                    navController.navigate("submitNewFood")
                },
                onNavigateToMap = {
                    navController.navigate("mapScreen")
                }

            )
        }


    }
}
// Top level screens
@Composable
fun SubmitScreen(auth: FirebaseAuth, context: Context, onNavigateToLogin: () -> Unit) {
    fun submitMeal(mealName:String, locationName: String, vegetarian: Boolean, rating: Int) {
        // trust and believe!
    }

    if (auth.currentUser == null) {
        Toast.makeText(context, "You cannot submit unless you are logged in.", Toast.LENGTH_LONG).show()
        onNavigateToLogin
    }
    else {
        Column(modifier = Modifier.fillMaxSize()) {
            val centered = Modifier.align(Alignment.CenterHorizontally)
            val mealName = remember { mutableStateOf(TextFieldValue()) }
            val mealLocation = remember { mutableStateOf(TextFieldValue()) }
            val rating = remember { mutableStateOf(0) }
            val isVegan = remember { mutableStateOf(true) }
            Spacer(modifier = Modifier.height(80.dp))
            TextField(
                label = { Text(text = "What did you eat?") },
                value = mealName.value,
                onValueChange = { mealName.value = it },
                modifier = centered
            )

            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                label = { Text("Where did you eat it?") },
                value = mealLocation.value,
                onValueChange = { mealLocation.value = it },
                modifier = centered
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Vegan?", modifier = centered)
            Checkbox(
                checked = isVegan.value,
                onCheckedChange = { isVegan.value = it },
                modifier = centered
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    submitMeal(
                        mealName.value.toString(),
                        mealLocation.value.toString(),
                        isVegan.value,
                        rating.value
                    )
                },
                modifier = centered,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text("Submit Meal", color = Color.White)
            }
        }
    }
}

@Composable
fun ListScreen() {
    Placeholder(component = "List View")
}

@Composable
fun SignupScreen(auth: FirebaseAuth, context: Context, listenerOwner: ComponentActivity) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }
        val vegan = remember { mutableStateOf(false) }
        Text(text = "Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Email Address") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password (6 characters or more)") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "Vegan?")
        Switch(modifier = Modifier.align(Alignment.CenterHorizontally), checked = vegan.value, onCheckedChange = {vegan.value = it});
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                          auth.createUserWithEmailAndPassword(
                              email.value.text.trim(),
                              password.value.text.trim()
                          ).addOnFailureListener {
                                  Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                              }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Sign Up", color = Color.White)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(auth: FirebaseAuth, context: Context) {
    val email = remember { mutableStateOf(TextFieldValue()) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Reset your password: ", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(60.dp))
        TextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            value = email.value,
            onValueChange = {email.value = it},
            label = {Text("email address")})
        Spacer(modifier = Modifier.height(60.dp))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {auth.sendPasswordResetEmail(email.value.text.trim())},
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
        ) {
            Text(text = "Send reset email", color = Color.White)
        }
    }
}

//sub-screens
@Composable
fun LoginScreen(
    onNavigateToForgotPass: () -> Unit,
    onNavigateToSignup: () -> Unit,
    auth: FirebaseAuth,
    context: Context
) {
    fun hashPassword(clearPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(clearPassword.toByteArray(Charsets.UTF_8))
        val hashedPassword = StringBuilder()
        for (byte in bytes) {
            hashedPassword.append(String.format("%02X", byte))
        }
        return hashedPassword.toString()
    }
    fun submitLoginAttempt(username: String, password: String): String {

        // magic! And by magic, I mean we're going to send an http request to our API asking for a user token
        // using a hashed password and username as a bargaining chip
        val postData = String.format("user=%s&pass=%s", username, hashPassword(password))
        val url = URL("google.com") //TODO this wont be our endpoint.
        // Google is not gonna give us a user token.

        val connection = url.openConnection()
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.setRequestProperty("Content-Length", postData.length.toString())
        // write to connection
        DataOutputStream(connection.getOutputStream()).use {
            it.writeBytes(postData)
        }
        //read response
        var responseBuilder = StringBuilder()
        BufferedReader(InputStreamReader(connection.getInputStream())) .use { response ->
            var line: String?
            while (response.readLine().also { line = it } != null) {
                responseBuilder.append(line)
            }
        }

        val responseJSON = JSONObject(responseBuilder.toString())

        if(responseJSON["status"].toString() != "200") {
            // Something went wrong.
            // More error handling in here please :)
            //TODO
        }
        else {
            return responseJSON["token"].toString()
        }
        return ""
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Button(onClick = onNavigateToSignup,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            Text("Sign Up")
        }
    }
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }

        Text(text = "Login Or Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Username") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(
                        email.value.text.trim(),
                        password.value.text.trim()
                    ).addOnFailureListener {
                        Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Login", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNavigateToForgotPass,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
            Text("Forgot Password?")
        }
    }
}

@Composable
fun LogoutScreen(onNavigateToMap: () -> Unit, auth: FirebaseAuth) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Would you like to logout?")
        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                auth.signOut()
                onNavigateToMap()
            }
        ) {
            Text("Yes")
        }
    }
}
// building blocks
@Composable
fun Placeholder(component: String) {
    Text(text = "$component goes here")
}

@Composable
fun TopBar(onMenuClicked: () -> Unit) {
    TopAppBar(
        title = {Text(text= "UMD Gluten Free", color=Color.White)},
        navigationIcon = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",

                // When clicked trigger onClick
                // Callback to trigger drawer open
                modifier = Modifier.clickable(onClick = onMenuClicked),
                tint = Color.White
            )
            // A button to move map to user's location
        },
        backgroundColor = Color(0xFFe21833)
    )
}

@Composable
fun Drawer(
    onNavigateToSettings: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        //first we want a logo or something so we can put the buttons closer to the
        // user's fingers
        val alignToCenter = Modifier.align(Alignment.CenterHorizontally)
        Image(
            painter = painterResource(id = R.drawable.umd_gluten_free_logo),
            contentDescription = "",
            modifier = alignToCenter.size(150.dp)
        )
        // This will get a few more buttons to open other screens
        //TODO increase font size, decrease padding
        TextButton(
            onClick = onNavigateToSubmit,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Submit New Meal", color=Color.Black)}
        TextButton(
            onClick = onNavigateToMap,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Map View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToList,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("List View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToSettings,
            modifier = alignToCenter.fillMaxWidth()
            ) {Text("Account Management", color=Color.Black)}
    }
}

@Composable
fun DrawerBody() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(cameraPositionState = CameraPositionState(position = CameraPosition(LatLng(38.9779990, -76.9287295), 15f, 0f, 0f)))
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    // to set menu closed by default
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val coroutineScope = rememberCoroutineScope()

    // Scaffold Composable
    Scaffold(

        // pass the scaffold state
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        // pass the topbar we created
        topBar = {
            TopBar(
                // When menu is clicked open the
                // drawer in coroutine scope
                onMenuClicked = {
                    coroutineScope.launch {
                        // to close use -> scaffoldState.drawerState.close()
                        scaffoldState.drawerState.open()
                    }
                })
        },


        // Pass the body in
        // content parameter

        // THIS COULD BE A PROBLEM >:O
        content = {
            DrawerBody()
        },

        // pass the drawer
        drawerContent = {
            Drawer(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToList = onNavigateToList,
                onNavigateToSubmit = onNavigateToSubmit,
                onNavigateToMap = onNavigateToMap
            )
        }
    )
}

