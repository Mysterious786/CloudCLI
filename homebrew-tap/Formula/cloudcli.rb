class Cloudcli < Formula
  desc "Multi-Database Backup CLI Tool - Backup to the cloud with one command"
  homepage "https://github.com/Mysterious786/CloudCLI"
  url "https://github.com/Mysterious786/CloudCLI/releases/download/v1.0.0/cloudcli.jar"
  sha256 "9edc2a1ac29b43dcf0318e2589729c20614e4d7088bcf8260e0f16215d25acb6"
  version "1.0.0"
  license "MIT"

  depends_on "openjdk@17"

  def install
    libexec.install "cloudcli.jar"

    (bin/"cloudcli").write <<~EOS
      #!/bin/bash
      exec "#{Formula["openjdk@17"].opt_bin}/java" -jar "#{libexec}/cloudcli.jar" "$@"
    EOS
  end

  test do
    assert_match "cloudcli", shell_output("#{bin}/cloudcli --version 2>&1", 0)
  end
end
