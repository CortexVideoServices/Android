package solutions.cvs.videoroom.conference.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import solutions.cvs.videoroom.R
import solutions.cvs.videoroom.base.BaseFragment
import solutions.cvs.videoroom.conference.ViewModel as ConferenceViewModel
import solutions.cvs.videoroom.databinding.CreateConferenceBinding
import solutions.cvs.videoroom.user.UserSession


class CreateFragment : BaseFragment() {

    private val userSession: UserSession by activityViewModels()
    private val conferenceVM: ConferenceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<CreateConferenceBinding>(
            inflater,
            R.layout.fragment_conference_create,
            container,
            false
        ).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.conferenceVM = conferenceVM
            it.userSession = userSession
        }.root
    }

}