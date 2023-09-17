## Android应用案例

### lateinit应用
```kotlin
class MainActivity2 : AppCompatActivity() {
    private lateinit var fragment: Fragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.findFragmentById(R.id.xxx)?.let { 
                fragment = it
            }
        }
        if (!::fragment.isInitialized) {
            fragment = MyFragment()
        }
        setContentView(R.layout.xxx)
        showFragment(supportFragmentManager, fragment)
    }
}
```