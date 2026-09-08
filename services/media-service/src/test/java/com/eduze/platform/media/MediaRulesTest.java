package com.eduze.platform.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eduze.platform.runtime.PlatformException;
import org.junit.jupiter.api.Test;

class MediaRulesTest {
    @Test
    void metadataCannotDisguiseExecutableAsPhotograph() {
        assertThatThrownBy(
                        () -> MediaRules.validateContent("image/jpeg", new byte[] {'M', 'Z', 0, 0}))
                .isInstanceOf(PlatformException.class);
    }

    @Test
    void rejectsUnsafeOrOversizedUpload() {
        assertThatThrownBy(() -> MediaRules.validateUpload("application/javascript", 200))
                .isInstanceOf(PlatformException.class);
        assertThatThrownBy(() -> MediaRules.validateUpload("image/jpeg", 30_000_000))
                .isInstanceOf(PlatformException.class);
        assertThat(MediaRules.extension("image/jpeg")).isEqualTo("jpg");
    }

    @Test
    void actualJpegSignatureAccepted() {
        MediaRules.validateContent(
                "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0});
    }
}
