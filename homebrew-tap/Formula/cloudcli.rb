class Cloudcli < Formula
  desc "Multi-Database Backup CLI Tool - Backup to the cloud with one command"
  homepage "https://github.com/saqlainansari/cloudcli"
  url "https://github.com/saqlainansari/cloudcli/releases/latest/download/cloudcli.jar"
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
    system "#{bin}/cloudcli", "--version"
  end
end
