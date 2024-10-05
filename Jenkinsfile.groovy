pipeline {
    agent any
    
    parameters {
        string(name: 'CONFIG_NAME', defaultValue: '', description: 'Enter the configuration name')
        string(name: 'IP_NUMBER', defaultValue: '', description: 'Enter the IP address')
        
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage ('Generate Public and Private Keys'){
            steps {
                script {
                    def keys_gen = sh(returnStatus: true, script:' wg genkey | tee /etc/wireguard/${CONFIG_NAME}_privatekey | wg pubkey | tee /etc/wireguard/${CONFIG_NAME}_publickey')
                   
                }
            }
        }

        stage('Edit config') {
            steps {
                script {
                    def public_key = sh(returnStdout: true, script: 'cat /etc/wireguard/${CONFIG_NAME}_publickey').trim()
                    def result = sh(returnStatus: true, script: """
                        {
                            echo ""
                            echo "#${CONFIG_NAME}"
                            echo "[Peer]"
                            echo "PublicKey = ${public_key}"
                            echo "AllowedIPs = ${IP_NUMBER}/32"
                        } >> "/etc/wireguard/wg0.conf"
                    """)

                    if (result == 0) {
                        println("Строки успешно добавлены в /etc/wireguard/wg0.conf.")
                    } else {
                        error('ERROR')
                    }
                }
            }
        }

        stage('Generate Config') {
            steps {
                script {
                    def private_key = sh(returnStdout: true, script: 'cat /etc/wireguard/${CONFIG_NAME}_privatekey').trim()
                    def public_key_server = sh(returnStdout: true, script: 'cat /etc/wireguard/server_publickey').trim()

                    println("[Interface]\nPrivateKey = ${private_key}\nAddress = ${IP_NUMBER}/32\nDNS = 8.8.8.8, 1.1.1.1\n\n[Peer]\nPublicKey = ${public_key_server}\nAllowedIPs = 0.0.0.0/0, ::/0\nEndpoint = 5.42.101.240:10886\nPersistentKeepalive = 20")   
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}
