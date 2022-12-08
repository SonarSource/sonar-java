load("github.com/SonarSource/cirrus-modules@v2", "cirrus_auth")

def main(ctx):
  return load_features(ctx, aws=dict(env_type="dev", cluster_name="CirrusCI-1-dev", subnet_id="subnet-0a6568c6e7d8107cf"))
