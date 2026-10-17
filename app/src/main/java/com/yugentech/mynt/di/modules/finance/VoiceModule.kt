package com.yugentech.mynt.di.modules.finance

import com.yugentech.mynt.voice.recorder.AndroidAudioRecorder
import com.yugentech.mynt.voice.repository.VoiceRepository
import com.yugentech.mynt.voice.repository.VoiceRepositoryImpl
import com.yugentech.mynt.voice.service.VoiceService
import com.yugentech.mynt.voice.viewmodel.VoiceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val voiceModule = module {

    single {
        VoiceService(
            functions = get()
        )
    }

    single<VoiceRepository> {
        VoiceRepositoryImpl(
            voiceService = get()
        )
    }

    // One recorder per voice session, since it owns a single AudioRecord instance
    factory {
        AndroidAudioRecorder(
            context = androidContext()
        )
    }

    viewModel {
        VoiceViewModel(
            voiceRepository = get(),
            audioRecorder = get()
        )
    }
}
