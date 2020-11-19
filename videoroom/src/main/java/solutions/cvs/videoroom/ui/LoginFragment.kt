package solutions.cvs.videoroom.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import solutions.cvs.videoroom.R
import solutions.cvs.videoroom.databinding.UserLoginBinding
import solutions.cvs.videoroom.UserSession


/**
 * Login fragment
 */
class LoginFragment : Fragment() {

    private val userSession: UserSession by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<UserLoginBinding>(inflater, R.layout.fragment_login, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.userSession = userSession
        }.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userSession.error.observe(viewLifecycleOwner, { error ->
            if (error != null)
                Snackbar.make(view, error, Snackbar.LENGTH_LONG)
                    .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            userSession.resetError()
                        }
                    }).show()
        })

        view.findViewById<Button>(R.id.button_2SignUp).setOnClickListener {
            findNavController().navigate(R.id.action_Login2SignUp)
        }
    }

}